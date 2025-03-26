package com.example.demo.dto;

import java.io.Serializable;
import lombok.Data;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

@Data
public class MessageDto implements Serializable {
    private String type; // "user" 或 "assistant"
    private String content;

    public MessageDto() {}

    public MessageDto(String type, String content) {
        this.type = type;
        this.content = content;
    }

    // 转换Spring AI Message到DTO
    public static MessageDto fromAiMessage(Message message) {
        MessageDto dto = new MessageDto();
        dto.setType(message.getMessageType().name().toLowerCase());
        dto.setContent(message.getText());
        return dto;
    }

    // 转回Spring AI Message
    public Message toAiMessage() {
        return switch (this.type) {
            case "user" -> new UserMessage(this.content);
            case "assistant" -> new AssistantMessage(this.content);
            default -> throw new IllegalArgumentException("Unknown message type");
        };
    }

    // getters/setters
}