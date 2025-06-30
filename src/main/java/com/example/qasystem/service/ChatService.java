package com.example.qasystem.service;

import com.example.qasystem.config.ModelProperties;
import com.example.qasystem.listener.CacheEvictionEvent;
import com.example.qasystem.mapper.ChatHistoryMapper;
import com.example.qasystem.mapper.ConversationMapper;
import com.example.qasystem.model.ChatHistory;
import com.example.qasystem.model.Conversation;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import com.example.qasystem.listener.CacheEvictionEvent;

import java.nio.file.AccessDeniedException;
import java.rmi.ServerException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    private final ChatClient chatClient;

    @Autowired
    private ChatHistoryMapper chatHistoryMapper;

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private final InMemoryChatMemory chatMemory = new InMemoryChatMemory();

    private static final String SYSTEM_ROLE = """
    你是一个智能助手，需要对用户的需求进行回复，请严格遵守以下规则：
    1. 使用口语化、温和的语气回答，严禁分点作答，被询问到你是谁，你就回答是一个智能助手
    2. 必须使用纯文本格式，绝对禁止使用任何Markdown语法（包括但不限于"#", "**", "```", ">", "-", "*", "`", "~", "[]", "()"等符号）
    3. 语言简洁专业，不超过500字
    4. 如果用户消息为空，回复"请给我一点提示，我会帮助您解决问题"
    5. 禁止以任何形式使用Markdown标题、列表、代码块、引用块等格式
    6. 所有回复内容必须保持为纯文本段落，不使用任何格式化符号
    7，如果需要分点作答，使用首先，其次，第一，第二的类型""";

    private static final String CHAT_HISTORY_CACHE_PREFIX = "chat_history_";
    private static final String CONVERSATION_CACHE_PREFIX = "conversation_";
    private static final Duration CACHE_TTL = Duration.ofHours(24); // Cache time-to-live

    public ChatService(ModelProperties modelProperties){
        this.chatClient = buildChatClient(
                modelProperties.getOpenai().getBaseUrl(),
                modelProperties.getOpenai().getApiKey(),
                modelProperties.getOpenai().getChat().getOptions()
        );
    }

    /**
     * 获取流式响应
     */
    @Transactional
    public Flux<String> getStreamingResponse(String prompt, Long userId, Long conversationId) {
        logger.info("Processing chat request for userId: {}, prompt: {}", userId, prompt);
        String cacheKey = CHAT_HISTORY_CACHE_PREFIX + "conversation_" + conversationId;
        String historyCacheKey = CHAT_HISTORY_CACHE_PREFIX + "conversation_" + conversationId;
        String conversationCacheKey = CONVERSATION_CACHE_PREFIX + userId;
        // 只查当前会话历史
        List<ChatHistory> chatHistoryList = (List<ChatHistory>) redisTemplate.opsForValue().get(historyCacheKey);
        if (chatHistoryList == null) {
            chatHistoryList = chatHistoryMapper.findByUserId(userId);
            if (chatHistoryList == null) {
                chatHistoryList = Collections.emptyList();
            }
            redisTemplate.opsForValue().set(historyCacheKey, chatHistoryList, CACHE_TTL);
        }

        // 取最近N条历史（如5条）
        int contextLimit = 5;
        List<ChatHistory> contextList = chatHistoryList.stream()
                .sorted(Comparator.comparing(ChatHistory::getVersion)) // 按顺序
                .skip(Math.max(0, chatHistoryList.size() - contextLimit))
                .collect(Collectors.toList());

        StringBuilder historyContext = new StringBuilder();
        for (ChatHistory h : contextList) {
            if (h.getUserMessage() != null && !h.getUserMessage().isBlank()) {
                historyContext.append("用户：").append(h.getUserMessage()).append("\n");
            }
            if (h.getAiResponse() != null && !h.getAiResponse().isBlank()) {
                historyContext.append("AI：").append(h.getAiResponse()).append("\n");
            }
        }
        // 拼接本次输入
        historyContext.append("用户：").append(prompt).append("\nAI：");

        long maxVersion = chatHistoryList.stream()
                .mapToLong(ChatHistory::getVersion)
                .max()
                .orElse(0);
        ChatHistory chatHistory = new ChatHistory();
        chatHistory.setUserId(userId);
        chatHistory.setUserMessage(prompt);
        chatHistory.setVersion(maxVersion + 1);
        chatHistory.setCreatedAt(LocalDateTime.now());

        // 创建新会话
        if (conversationId == null) {
            Conversation conversation = new Conversation();
            conversation.setUserId(userId);
            conversation.setName(prompt.substring(0, Math.min(prompt.length(), 10)));
            conversation.setStartTime(new Date());
            conversationMapper.insertConversation(conversation);
            chatHistory.setConversationId(conversation.getId());
            // Invalidate conversation cache
            redisTemplate.delete(conversationCacheKey);
            redisTemplate.delete(cacheKey);
        } else {
            chatHistory.setConversationId(conversationId);
        }
        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setName(prompt.substring(0, Math.min(prompt.length(), 10)));
        conversation.setUpdateTime(new Date());
        conversationMapper.updateConversation(conversation);
        redisTemplate.delete(conversationCacheKey);
        try {
            chatHistoryMapper.insertChatHistory(chatHistory);
            logger.debug("保存历史记录: {}", prompt);
            // 删除缓存
            redisTemplate.delete(historyCacheKey);
            redisTemplate.delete(cacheKey);
        } catch (Exception e) {
            logger.error("保存历史记录失败: {}", prompt, e);
            throw e;
        }

        StringBuilder aiResponseBuilder = new StringBuilder();
        return chatClient.prompt()
                .user(historyContext.toString())
                .system(SYSTEM_ROLE)
                .stream()
                .content()
                .bufferUntil(response -> response.endsWith(".") || response.endsWith("！") || response.endsWith("?") || response.endsWith("。")) // 按句子结束符聚合
                .map(chunks -> {
                    String combined = chunks.stream()
                            .map(this::cleanMarkdown)
                            .filter(s -> !s.trim().isEmpty())
                            .collect(Collectors.joining(""));
                    return combined.trim(); // 直接返回内容
                })
                .filter(response -> !response.isEmpty())
                .doOnNext(response -> {
                    logger.debug("Sending chunk: {}", response);
                    aiResponseBuilder.append(response.replace("data: ", ""));
                })
                .doOnError(error -> logger.error("Error in streaming response for prompt: {}", prompt, error))
                .doOnComplete(() -> {
                    String completeAiResponse = aiResponseBuilder.toString();

                    // 检查响应是否完整
                    if (completeAiResponse.length() < 50) { // 根据业务调整阈值
                        logger.warn("回应太短: {}", completeAiResponse);
                        completeAiResponse += "（回答可能不完整，请尝试重新提问）";
                    }

                    // 检查是否包含违禁格式
                    if (completeAiResponse.contains("**") ||
                            completeAiResponse.contains("#") ||
                            completeAiResponse.contains("```")) {
                        logger.error("存在Markdown块!");
                        completeAiResponse = cleanMarkdown(completeAiResponse);
                    }

                    logger.info("完成对话的userId: {}, response: {}", userId, completeAiResponse);

                    chatHistory.setAiResponse(completeAiResponse);
                    try {
                        chatHistoryMapper.updateChatHistory(chatHistory);
                        logger.debug("更新AI回复");
                        // Invalidate cache after update
                        redisTemplate.delete(historyCacheKey);
                        redisTemplate.delete(cacheKey);
                    } catch (Exception e) {
                        logger.error("更新AI回复失败: {}", completeAiResponse, e);
                    }
                });
    }
    /**
     * 获取历史记录
     */
    public List<ChatHistory> getChatHistory(Long userId) {
        logger.debug("Retrieving chat history for userId: {}", userId);
        String cacheKey = CHAT_HISTORY_CACHE_PREFIX + userId;
        List<ChatHistory> history = (List<ChatHistory>) redisTemplate.opsForValue().get(cacheKey);
        if (history == null) {
            try {
                history = chatHistoryMapper.findByUserId(userId);
                redisTemplate.opsForValue().set(cacheKey, history, CACHE_TTL);
                logger.info("Retrieved {} chat history entries for userId: {}", history.size(), userId);
            } catch (Exception e) {
                logger.error("Failed to retrieve chat history for userId: {}", userId, e);
                throw e;
            }
        }
        return history;
    }

    /**
     * 创建新会话
     */
    public Long createNewConversation(Long userId) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setName("新建会话");
        conversation.setStartTime(new Date());
        conversationMapper.insertConversation(conversation);
        // Invalidate conversation cache
        redisTemplate.delete(CONVERSATION_CACHE_PREFIX + userId);
        return conversation.getId();
}

    /**
     * 获取用户会话列表
     */
    public List<Conversation> getConversationsByUserId(Long userId) {
        String cacheKey = CONVERSATION_CACHE_PREFIX + userId;
        List<Conversation> conversations = (List<Conversation>) redisTemplate.opsForValue().get(cacheKey);
        if (conversations == null) {
            conversations = conversationMapper.findByUserId(userId);
            redisTemplate.opsForValue().set(cacheKey, conversations, CACHE_TTL);
        }
        return conversations;
    }

    /**
     * 获取会话历史记录
     */
    public List<ChatHistory> getChatHistoryByConversationId(Long conversationId) {
        String cacheKey = CHAT_HISTORY_CACHE_PREFIX + "conversation_" + conversationId;
        List<ChatHistory> history = (List<ChatHistory>) redisTemplate.opsForValue().get(cacheKey);
        if (history == null) {
            history = chatHistoryMapper.findByConversationId(conversationId);
            redisTemplate.opsForValue().set(cacheKey, history, CACHE_TTL);
        }
        return history;
    }

    /**
     * 生成会话摘要
     */
    public List<ChatHistory> generateConversationSummary(Long conversationId) {
        List<ChatHistory> chatHistoryList = getChatHistoryByConversationId(conversationId);
        if (chatHistoryList == null || chatHistoryList.isEmpty()) {
            return null;
        }
        return chatHistoryList;
    }

    /**
     * 删除会话和历史记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteConversationAndHistory(Long conversationId, Long userId) {
        // 1. 校验会话归属
        if (!isUserOwnsConversation(userId, conversationId)) {
            throw new RuntimeException("无权删除他人会话");
        }

        // 2. 删除数据库记录
        chatHistoryMapper.deleteByConversationId(conversationId);
        conversationMapper.deleteConversationById(conversationId);

        // 3. 异步清理缓存（通过事务监听或MQ）
        String cacheKey = CHAT_HISTORY_CACHE_PREFIX + "conversation_" + conversationId;
        String conversationCacheKey = CONVERSATION_CACHE_PREFIX + userId;
        String historyCacheKey = CHAT_HISTORY_CACHE_PREFIX + userId;
        redisTemplate.delete(cacheKey);
        redisTemplate.delete(conversationCacheKey);
        redisTemplate.delete(historyCacheKey);
    }

    private ChatClient buildChatClient(String baseUrl, String apiKey, ModelProperties.Options options) {
        OpenAiApi api = new OpenAiApi(baseUrl, apiKey);
        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
                .model(options.getModel())
                .temperature(options.getTemperature())
                .maxTokens(options.getMaxTokens())
                .topP(0.95)
//                .stop(Arrays.asList("#", "**", "```"))
                .build();
        return ChatClient.builder(new OpenAiChatModel(api, chatOptions))
                .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
                .defaultSystem(SYSTEM_ROLE)
                .build();
    }

    // 新增Markdown清理方法
    private String cleanMarkdown(String text) {
        return text.replace("#", "")
                .replace("**", "")
                .replace("```", "")
                .replace(">", "")
                .replace("---", "")
                .replace("- ", "")
                .replace("* ", "")
                .replace("data: ","")
                .replace("data:","")
                .replace("•", "");
    }

    private boolean isUserOwnsConversation(Long userId, Long conversationId) {
        Conversation conversation = conversationMapper.findById(String.valueOf(conversationId));
        return conversation != null && conversation.getUserId().equals(userId);
    }

}
