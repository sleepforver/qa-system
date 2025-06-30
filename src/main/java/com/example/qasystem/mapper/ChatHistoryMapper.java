package com.example.qasystem.mapper;

import com.example.qasystem.model.ChatHistory;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ChatHistoryMapper {
    void insertChatHistory(ChatHistory chatHistory);
    /**
     * 查询
     * @param userId
     * @return
     */
    List<ChatHistory> findByUserId(Long userId);

    /**
     * 更新
     * @param chatHistory
     */
    void updateChatHistory(ChatHistory chatHistory);

    /**
     * 根据会话ID查询
     * @param conversationId
     * @return
     */
    List<ChatHistory> findByConversationId(Long conversationId);

    /**
     * 根据会话ID删除
     * @param conversationId
     */
    void deleteByConversationId(Long conversationId);
}
