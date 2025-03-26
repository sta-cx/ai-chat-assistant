package com.example.demo.vo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * 请求类DTO
 * @author
 */
@Data
public class OllamaChatRequest {
    private String model;
    private List<Message> messages; // 消息列表
    private boolean stream = false;
    private Map<String, Object> options = new HashMap<>();

    @Data
    public static class Message {
        private String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}