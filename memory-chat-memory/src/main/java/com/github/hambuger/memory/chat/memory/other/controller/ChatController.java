package com.github.hambuger.memory.chat.memory.other.controller;

import com.alibaba.fastjson.JSON;
import com.github.hambuger.memory.chat.memory.chat.CommonMessageHandler;
import com.github.hambuger.memory.chat.memory.chat.model.BaseReceiveMessage;
import com.github.hambuger.memory.chat.memory.chat.model.ChatResponse;
import com.github.hambuger.memory.chat.memory.other.util.MyHttpUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;


/**
 * @author hamburger
 * @since 2024/6/15
 */
@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private CommonMessageHandler commonMessageHandler;


    @PostMapping("/message")
    @ResponseBody
    public ChatResponse message(@RequestBody BaseReceiveMessage memoryDTO) {
        return commonMessageHandler.receiveNewMsg(memoryDTO);
    }


    @GetMapping("/send/register")
    public Boolean register(@RequestParam String channelEnum, @RequestParam String registerUrl) {
        commonMessageHandler.registerSendTool(channelEnum, param -> MyHttpUtils.post(registerUrl, JSON.toJSONString(param)));
        return true;
    }

}
