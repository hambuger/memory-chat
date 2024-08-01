package com.github.hambuger.memory.chat.memory.chat.dto;

/**
 * @author hanjiabao
 * @since 2024/7/23
 */
public enum ChatSceneEnum {

    NORMAL_USER("正常好友沟通"),
    NORMAL_GROUP("正常群沟通"),
    SCHEDULE("退避尝试发起对话"),
    NEWS_SCHEDULE("定时根据热点尝试发起对话"),
    TASK("延迟任务"),

    ;

    private String desc;


    ChatSceneEnum(String desc) {
        this.desc = desc;
    }

}
