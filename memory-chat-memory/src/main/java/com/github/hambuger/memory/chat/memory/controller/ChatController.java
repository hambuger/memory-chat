package com.github.hambuger.memory.chat.memory.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.github.hambuger.memory.chat.memory.memory.model.BaseMemoryDTO;
import com.github.hambuger.memory.chat.memory.chat.ChatCompletionsApi;
import com.github.hambuger.memory.chat.memory.chat.dto.ChatResponse;


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
        return chatCompletionsApi.chat(memoryDTO);
    }

}
