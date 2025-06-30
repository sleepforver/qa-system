package com.example.qasystem.controller;

import com.example.qasystem.mapper.ConversationMapper;
import com.example.qasystem.model.ChatHistory;
import com.example.qasystem.model.Conversation;
import com.example.qasystem.model.User;
import com.example.qasystem.model.dto.ConversationSummary;
import com.example.qasystem.service.ChatService;
import com.example.qasystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.rmi.ServerException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@CrossOrigin
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    private final ChatService chatService;
    private final UserService userService;

    @Autowired
    private ConversationMapper conversationMapper;

    /**
     * 聊天页面
     */
    @GetMapping("/chat")
    public String chatPage(Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.debug("向用户展示聊天界面: {}", username);

        User user = userService.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        Long userId = user.getId();
        model.addAttribute("history", chatService.getChatHistory(userId));
        return "chat";
    }

    /**
     * 聊天接口
     */
    @PostMapping(value = "/api/chat", produces = "text/event-stream")
    @ResponseBody
    public Flux<String> chatStream(@RequestBody Map<String, String> payload) {
        String prompt = payload.get("prompt");
        // 安全地解析 conversationId
        String conversationIdStr = payload.get("conversationId");
        Long conversationId = conversationMapper.findById(conversationIdStr) != null ? Long.parseLong(conversationIdStr) : null;
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("收到用户请求: {}, 提示词: {}", username, prompt);

        Long userId = userService.findByUsername(username).getId();
        return chatService.getStreamingResponse(prompt, userId, conversationId);
    }

    /**
     * 新建会话接口
     */
    @PostMapping("/api/new-conversation")
    @ResponseBody
    public Map<String, Long> newConversation() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Long userId = userService.findByUsername(username).getId();
        Long conversationId = chatService.createNewConversation(userId);
        return Collections.singletonMap("conversationId", conversationId);
    }

    /**
     * 获取会话列表接口
     */
    @GetMapping("/api/conversations")
    @ResponseBody
    public List<Conversation> conversations() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Long userId = userService.findByUsername(username).getId();
        return chatService.getConversationsByUserId(userId);
    }

    /**
     * 获取会话历史接口
     */
    @GetMapping("/api/chat-history/{conversationId}")
    @ResponseBody
    public List<ChatHistory> chatHistory(@PathVariable Long conversationId) {
        return chatService.getChatHistoryByConversationId(conversationId);
    }

    /**
     * 获取会话摘要接口
     */
    @GetMapping("api/conversation/{conversationId}/summary")
    @ResponseBody
    public List<ChatHistory> getConversationSummary(@PathVariable Long conversationId) {
        return chatService.generateConversationSummary(conversationId);
    }

    /**
     * 删除会话接口
     */
    @DeleteMapping("/api/delete-conversation/{conversationId}")
    @ResponseBody
    public ResponseEntity<Void> deleteConversation(@PathVariable Long conversationId) throws ServerException {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Long userId = userService.findByUsername(username).getId();

        chatService.deleteConversationAndHistory(conversationId, userId);
        return ResponseEntity.ok().build();
    }

}
