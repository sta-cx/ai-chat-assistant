package com.example.demo.service;

import com.example.demo.dto.MessageDto;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 对话管理类
 *
 * @author
 */
@Service
public class ConversationService {

    private static final String REDIS_KEY_PREFIX = "conversation:";
    private static final int MAX_HISTORY = 10;  // 保留最近10条对话
    private static final long TTL_HOURS = 24;   // 数据保留24小时

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // 添加消息到对话历史
    public void addMessage(String conversationId, String role, String content) {
        String key = getRedisKey(conversationId);
        MessageDto newMessage = new MessageDto(role, content);

        // 使用Redis List存储
        redisTemplate.opsForList().rightPush(key, newMessage);

        // 修剪列表长度
        redisTemplate.opsForList().trim(key, -MAX_HISTORY, -1);

        // 设置过期时间
        redisTemplate.expire(key, TTL_HOURS, TimeUnit.HOURS);
    }

    // 获取对话历史
    public List<MessageDto> getHistory(String conversationId) {
        String key = getRedisKey(conversationId);
        List<Object> rawList = redisTemplate.opsForList().range(key, 0, -1);

        return rawList.stream()
                .map(obj -> (MessageDto) obj)
                .collect(Collectors.toList());
    }

    // 删除对话历史
    public void clearHistory(String conversationId) {
        redisTemplate.delete(getRedisKey(conversationId));
    }

    private String getRedisKey(String conversationId) {
        return REDIS_KEY_PREFIX + conversationId;
    }

//    @Data
//    @AllArgsConstructor
//    @NoArgsConstructor
//    public static class Message implements Serializable {
//
//        private String role;
//        private String content;
//    }
}