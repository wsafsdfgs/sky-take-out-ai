package com.sky.aicustomerservice.entiy;

import lombok.Data;

@Data
public class ChatMemoryRecord {
    private String sessionId;
    private String role;
    private String content;
    private Long createTime;
    private Long userId;
}
