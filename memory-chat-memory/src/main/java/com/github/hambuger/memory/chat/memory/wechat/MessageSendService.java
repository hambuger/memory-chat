package com.github.hambuger.memory.chat.memory.wechat;

import com.github.hambuger.memory.chat.memory.functionCall.aop.FunctionCallRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MessageSendService {


    @FunctionCallRegistry(functionDesc = "处理发送微信消息请求")
    public boolean sendWechatMessage(SendMessageRequest sendMessageRequest){
        return true;
    }

}
