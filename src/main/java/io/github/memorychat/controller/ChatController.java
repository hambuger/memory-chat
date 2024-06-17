package io.github.memorychat.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.github.memorychat.memory.model.BaseMemoryDTO;
import io.github.memorychat.wechat.ChatCompletionsApi;
import io.github.memorychat.wechat.dto.ChatResponse;


/**
 * @author hamburger
 * @since 2024/6/15
 */
@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private ChatCompletionsApi chatCompletionsApi;


    @PostMapping("/wechat")
    @ResponseBody
    public ChatResponse wechat(@RequestBody BaseMemoryDTO memoryDTO) {
        chatCompletionsApi.convertAudio2TextMsg(memoryDTO);
        return ChatCompletionsApi.chat(memoryDTO);
    }

}
