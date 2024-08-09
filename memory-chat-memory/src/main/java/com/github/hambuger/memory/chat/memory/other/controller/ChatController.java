package com.github.hambuger.memory.chat.memory.other.controller;

import com.github.hambuger.memory.chat.memory.chat.ChatCompletionsApi;
import com.github.hambuger.memory.chat.memory.chat.model.ChatResponse;
import com.github.hambuger.memory.chat.memory.memory.model.BaseMemoryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


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
