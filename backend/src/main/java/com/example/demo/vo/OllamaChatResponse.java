package com.example.demo.vo;

import lombok.Data;

/**
 * 响应DTO
 */
@Data
public class OllamaChatResponse {
    private Message message;
    private String createdAt;
    private boolean done;

    @Data
    public static class Message {
        private String role;
        private String content;
    }

}
