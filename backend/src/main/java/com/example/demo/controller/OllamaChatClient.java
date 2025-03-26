package com.example.demo.controller;

import com.example.demo.service.OllamaService;
import java.util.Map;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Flux;

@CrossOrigin(
        origins = "*",
        allowedHeaders = "*",
        exposedHeaders = "Content-Type"
)
@RestController
public class OllamaChatClient {

    @Autowired
    private OllamaChatModel ollamaChatModel;


    @Autowired
    private OllamaService ollamaService;

    @RequestMapping(value = "/ai/ollama")
    public Object ollama(@RequestParam(value = "msg") String msg) {
        String called = ollamaChatModel.call(msg);
        return called;
    }

    @PostMapping("/ask")
    public String continueConversation(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String userInput = request.get("message");
        String response = ollamaService.chat(userId, userInput);
        return response;
    }

    @PostMapping("/streamAsk")
    public Flux<ChatResponse> streamAsk(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String userInput = request.get("message");
        return ollamaService.streamChat(userId, userInput);
    }

    /**
     * 逐字符流式响应API，提供类似打字效果（GET方法）
     */
    @GetMapping(value = "/typingAsk", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> typingAsk(@RequestParam String userId, @RequestParam String message) {
        return ollamaService.typingChat(userId, message);
    }

    /**
     * 逐字符流式响应API，提供类似打字效果和模型思考过程（POST方法）
     */
    @PostMapping(value = "/typingAsk", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> typingAskPost(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String message = request.get("message");
        return ollamaService.typingChat(userId, message);
    }
}
