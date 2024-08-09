package com.github.hambuger.memory.chat.memory.chat.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChatMember implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;

    private boolean groupFlag;
}
