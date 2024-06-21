package com.github.hambuger.memory.chat.memory.chat.dto;

/**
 * @author hamburger
 * @since 2024/6/18
 */
public enum CreatorEnum {

    USER("USER", "单用户", "", ""),

    GROUP("GROUP", "群聊", "", ""),

    Andrew("Andrew", "AI", "000001", "Andrew"),

    REFLECTION("REFLECTION", "反思归纳", "000002", "REFLECTION"),
    ;


    private String type;

    private String desc;

    private String userId;

    private String userName;


    public String getType() {
        return type;
    }


    public String getUserId() {
        return userId;
    }


    public String getUserName() {
        return userName;
    }


    CreatorEnum(String type, String desc, String userId, String userName) {
        this.type = type;
        this.desc = desc;
        this.userId = userId;
        this.userName = userName;
    }

}
