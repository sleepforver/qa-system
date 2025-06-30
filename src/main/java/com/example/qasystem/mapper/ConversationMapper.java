package com.example.qasystem.mapper;

import com.example.qasystem.model.Conversation;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ConversationMapper {

    /**
     * 插入一个会话
     *
     * @param conversation
     */
    void insertConversation(Conversation conversation);

    /**
     * 根据用户id查询会话
     *
     * @param userId
     * @return
     */
    List<Conversation> findByUserId(Long userId);

    /**
     * 根据会话id查询会话
     *
     * @param conversationId
     * @return
     */
    Conversation findById(String conversationId);

    /**
     * 更新一个会话
     *
     * @param conversation
     */
    void updateConversation(Conversation conversation);

    /**
     * 删除一个会话
     *
     * @param conversationId
     */
    void deleteConversationById(Long conversationId);
}
