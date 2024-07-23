package com.github.hambuger.memory.chat.memory.wechat;

import com.github.hambuger.memory.chat.memory.chat.dto.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.functionCall.aop.FunctionCallRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MessageSendService {


    @FunctionCallRegistry(functionDesc = "处理回复消息,所有对用户的回复都应该使用这个方法。而且如果不需要回复处理，也可以通过这个方法告知。如果要回复多条信息，更应该使用这个方法"
            , scene = {ChatSceneEnum.NORMAL_USER, ChatSceneEnum.NORMAL_GROUP, ChatSceneEnum.SCHEDULE, ChatSceneEnum.NEWS_SCHEDULE})
    public boolean replyMessageProcessing(SendMessageRequest sendMessageRequest){
        return true;
    }

}
