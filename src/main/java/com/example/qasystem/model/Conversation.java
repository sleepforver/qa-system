package com.example.qasystem.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Conversation {
    private Long id;
    private Long userId;
    private String name;
    private Date startTime;
    private Date updateTime;
}
