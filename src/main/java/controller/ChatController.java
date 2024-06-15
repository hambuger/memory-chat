package controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import memory.model.BaseMemoryDTO;
import wechat.ChatCompletionsApi;


/**
 * @author hamburger
 * @since 2024/6/15
 */
@RestController
@RequestMapping("/chat")
public class ChatController {
    @PostMapping("/wechat")
    public String wechat(@RequestBody BaseMemoryDTO memoryDTO) {
        return ChatCompletionsApi.chat(memoryDTO);
    }

}
