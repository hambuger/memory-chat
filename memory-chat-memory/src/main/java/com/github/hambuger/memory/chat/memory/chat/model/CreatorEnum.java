package com.github.hambuger.memory.chat.memory.chat.model;

/**
 * @author hamburger
 * @since 2024/6/18
 */
public enum CreatorEnum {

    USER("USER", "", ""),

    GROUP("GROUP", "", ""),

    Andrew("Andrew", "000001", "Andrew"),

    REFLECTION("REFLECTION", "000002", "REFLECTION"),
    ;


    private String type;

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


    CreatorEnum(String type, String userId, String userName) {
        this.type = type;
        this.userId = userId;
        this.userName = userName;
    }

}
