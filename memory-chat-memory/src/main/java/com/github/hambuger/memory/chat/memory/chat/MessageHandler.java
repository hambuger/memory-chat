package com.github.hambuger.memory.chat.memory.chat;

import com.github.hambuger.memory.chat.memory.chat.model.BaseReceiveMessage;
import com.github.hambuger.memory.chat.memory.chat.model.ChatResponse;
import com.github.hambuger.memory.chat.memory.chat.model.SendChannelMessageRequest;

public interface MessageHandler {

    ChatResponse receiveNewMsg(BaseReceiveMessage msg);

    void sendMessage(SendChannelMessageRequest request);

}
