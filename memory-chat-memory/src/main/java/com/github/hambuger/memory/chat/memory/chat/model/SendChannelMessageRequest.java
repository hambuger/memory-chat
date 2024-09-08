package com.github.hambuger.memory.chat.memory.chat.model;

import com.github.hambuger.memory.chat.memory.chat.message.SendMessage;
import lombok.Data;


@Data
public class SendChannelMessageRequest extends SendMessage {

    private String toUserId;

    private String filePath;

    private String channelEnum;

}
