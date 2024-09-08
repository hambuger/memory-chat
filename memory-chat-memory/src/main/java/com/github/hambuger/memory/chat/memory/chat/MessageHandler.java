package com.github.hambuger.memory.chat.memory.chat;

import com.github.hambuger.memory.chat.memory.chat.model.BaseReceiveMessage;
import com.github.hambuger.memory.chat.memory.chat.model.SendChannelMessageRequest;

public interface MessageHandler {

    void receiveNewMsg(BaseReceiveMessage msg);

    void sendMessage(SendChannelMessageRequest request);

}
