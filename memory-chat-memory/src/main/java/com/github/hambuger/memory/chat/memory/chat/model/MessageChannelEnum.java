package com.github.hambuger.memory.chat.memory.chat.model;

public enum MessageChannelEnum {

    WECHAT,
    AUDIO;


    public static MessageChannelEnum from(String channel) {
        for (MessageChannelEnum value : MessageChannelEnum.values()) {
            if (value.name().equalsIgnoreCase(channel)) {
                return value;
            }
        }
        return null;
    }

}
