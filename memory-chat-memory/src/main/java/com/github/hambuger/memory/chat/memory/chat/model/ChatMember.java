package com.github.hambuger.memory.chat.memory.chat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMember implements Serializable {

    private static final long serialVersionUID = 20240908L;

    private String name;

    private boolean groupFlag;

    private String sendUserId;

    private String channelScene;
}
