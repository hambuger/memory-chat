package com.github.hambuger.memory.chat.memory.chat.model;

import com.github.hambuger.memory.chat.memory.memory.model.BaseMemoryDTO;
import lombok.Data;

@Data
public class BaseReceiveMessage extends BaseMemoryDTO {


    private String receiveMessageUserId;

    private String channelEnum;
}
