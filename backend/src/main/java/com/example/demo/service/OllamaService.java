package com.example.demo.service;


import com.example.demo.dto.MessageDto;
import com.example.demo.vo.OllamaChatRequest;
import com.example.demo.vo.OllamaChatResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Duration;

/**
 * 向本地ds发送消息service
 *
 * @author
 */
@Service
public class OllamaService {
    //private static final String OLLAMA_CHAT_API = "http://localhost:11434/api/chat";

    @Autowired
    private OllamaChatModel ollamaChatModel;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ConversationService conversationService;

    public String chat(String conversationId, String userInput) {
        // 1. 获取历史对话
        List<MessageDto> messageDtos = conversationService.getHistory(conversationId);
        List<Message> messages = messageDtos.stream().map(MessageDto::toAiMessage)
                .collect(Collectors.toList());
        messages.add(new UserMessage(userInput));
        ChatResponse response = ollamaChatModel.call(new Prompt(messages));
        // 6. 保存对话历史
        conversationService.addMessage(conversationId, "user", userInput);
        conversationService.addMessage(conversationId, "assistant",
                response.getResult().getOutput().getText());
        return response.getResult().getOutput().getText();
    }

    public Flux<ChatResponse> streamChat(String conversationId, String userInput) {
        // 1. 获取历史对话
        List<MessageDto> messageDtos = conversationService.getHistory(conversationId);
        List<Message> messages = messageDtos.stream().map(MessageDto::toAiMessage)
                .collect(Collectors.toList());
        messages.add(new UserMessage(userInput));

        // 2. 立即保存用户输入到对话历史
        conversationService.addMessage(conversationId, "user", userInput);

        // 3. 创建流式响应
        Flux<ChatResponse> stream = ollamaChatModel.stream(new Prompt(messages));

        // 4. 收集完整响应并保存
        StringBuilder fullResponse = new StringBuilder();

        return stream
                .doOnNext(response -> {
                    // 累积响应文本
                    String text = response.getResult().getOutput().getText();
                    fullResponse.append(text);
                })
                .doOnComplete(() -> {
                    // 流结束时保存完整的助手回复到对话历史
                    conversationService.addMessage(conversationId, "assistant",
                            fullResponse.toString());
                });
    }

    /**
     * 提供逐字符打字效果的聊天响应，同时展示模型思考过程
     *
     * @param conversationId 会话ID
     * @param userInput      用户输入
     * @return 字符级别的流式响应，包含思考过程和最终回复
     */
    public Flux<String> typingChat(String conversationId, String userInput) {
        // 1. 获取历史对话
        List<MessageDto> messageDtos = conversationService.getHistory(conversationId);
        List<Message> messages = messageDtos.stream().map(MessageDto::toAiMessage)
                .collect(Collectors.toList());

        // 2. 立即保存用户输入到对话历史
        conversationService.addMessage(conversationId, "user", userInput);

        // 3. 拷贝原始消息列表用于实际执行
        List<Message> executionMessages = new java.util.ArrayList<>(messages);
        executionMessages.add(new UserMessage(userInput));

        // 4. 创建思考提示
        UserMessage thinkingPrompt = new UserMessage(
                "请分析以下问题，提供思考过程，但不要给出最终答案：" + userInput
        );
        List<Message> thinkingMessages = new java.util.ArrayList<>(messages);
        thinkingMessages.add(thinkingPrompt);

        // 5. 获取思考过程（向模型请求思考过程）
        Flux<ChatResponse> thinkingStream = ollamaChatModel.stream(new Prompt(thinkingMessages));
        StringBuilder thinkingResponse = new StringBuilder();

        // 6. 创建实际回答的流
        Flux<ChatResponse> answerStream = ollamaChatModel.stream(new Prompt(executionMessages));
        StringBuilder fullResponse = new StringBuilder();

        // 7. 序列化思考过程和回答
        return Flux.concat(
                // 先发送thinking流（使用正确的SSE格式）
            /*thinkingStream.takeUntil(response -> {
                        // 判断思考流是否应该结束（比如达到一定长度或包含特定内容）
                        String text = response.getResult().getOutput().getText();
                        thinkingResponse.append(text);
                        // 如果包含"总结"或思考长度超过500，认为思考结束
                        return thinkingResponse.toString().contains("总结")
                                || thinkingResponse.length() > 500;
                    })*/
                thinkingStream
                        .map(response -> {
                            String text = response.getResult().getOutput().getText();
                            // 正确的SSE格式: data: [内容]\n\n
                            return "data: " + text + "\n\n";
                        })
                        .startWith("data: [THINKING_START]\n\n") // 开始标记
                        .concatWith(Flux.just("data: [THINKING_END]\n\n")), // 结束标记

                // 然后发送实际回答流
                answerStream
                        .map(response -> {
                            String text = response.getResult().getOutput().getText();
                            fullResponse.append(text);
                            // 以5-10字符为单位发送，而不是单字符，减少网络开销
                            return "data: " + text + "\n\n";
                        })
                        .doOnComplete(() -> {
                            // 流结束时保存完整的助手回复到对话历史
                            conversationService.addMessage(conversationId, "assistant",
                                    fullResponse.toString());
                        })
                        .concatWith(Flux.just("data: [DONE]\n\n"))
        );
    }

    /**
     * 根据用户输入和对话历史生成模型思考内容
     *
     * @param userInput 用户输入
     * @param messages  对话历史
     * @return 结构化的思考过程
     */
    private String generateThinkingContent(String userInput, List<Message> messages) {
        // 实际应用中可以结合上下文做更复杂的思考内容生成
        StringBuilder thinking = new StringBuilder();
        thinking.append("分析用户问题: \"").append(userInput).append("\"\n\n");

        // 添加一些上下文分析
        thinking.append("对话上下文:\n");
        int contextMsgCount = Math.min(messages.size() - 1, 3); // 最多显示3条上下文消息，排除当前问题
        for (int i = messages.size() - contextMsgCount - 1; i < messages.size() - 1; i++) {
            if (i >= 0) {
                Message msg = messages.get(i);
                String role = msg instanceof UserMessage ? "user" : "assistant";
                String content = msg.toString();

                // 提取消息内容，处理可能的字符串格式问题
                if (content.contains("content=")) {
                    content = content.substring(content.indexOf("content=") + 8);
                    if (content.contains(",")) {
                        content = content.substring(0, content.indexOf(","));
                    } else if (content.contains(")")) {
                        content = content.substring(0, content.indexOf(")"));
                    }
                }

                thinking.append("- ").append(role).append(": ")
                        .append(content.length() > 50 ?
                                content.substring(0, 50) + "..." :
                                content)
                        .append("\n");
            }
        }
        thinking.append("\n");

        // 添加思考步骤
        thinking.append("思考步骤:\n");
        thinking.append("1. 理解用户意图和问题核心\n");
        thinking.append("2. 检索相关知识和上下文信息\n");
        thinking.append("3. 分析最佳回答策略\n");
        thinking.append("4. 确保回答全面、准确、有帮助\n");
        thinking.append("5. 组织清晰的回复结构\n\n");

        // 添加针对问题的分析
        thinking.append("问题分析:\n");

        // 基于问题关键词进行简单分析
        if (userInput.contains("如何") || userInput.contains("怎么")) {
            thinking.append("这是一个寻求方法或解决方案的问题，需要提供步骤性指导。\n");
        } else if (userInput.contains("为什么") || userInput.contains("原因")) {
            thinking.append("这是一个寻求解释或原因的问题，需要提供因果关系分析。\n");
        } else if (userInput.contains("区别") || userInput.contains("对比") || userInput.contains(
                "比较")) {
            thinking.append("这是一个比较类问题，需要进行对比分析，列出相同点和不同点。\n");
        } else if (userInput.contains("定义") || userInput.contains("是什么") || userInput.contains(
                "概念")) {
            thinking.append("这是一个概念解释类问题，需要提供清晰的定义和相关背景。\n");
        } else {
            thinking.append("这是一个需要综合分析的问题，将从多个角度进行思考。\n");
        }

        // 添加临时回答规划
        thinking.append("\n回答规划:\n");
        thinking.append("1. 首先直接回应用户的核心问题\n");
        thinking.append("2. 提供相关的背景信息或上下文\n");
        thinking.append("3. 详细展开主要内容，保持逻辑性\n");
        thinking.append("4. 总结关键点，确保信息完整\n");

        return thinking.toString();
    }
}