package com.example.qasystem.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatHistory {
    private Long id;
    private Long userId;
    private String userMessage;
    private String aiResponse;
    private Long version;
    private LocalDateTime createdAt;
    private Long conversationId;
}
