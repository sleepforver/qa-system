package com.example.qasystem.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversationSummary {
    private String conversationId;
    private String name;
    private LocalDateTime startTime;

}
