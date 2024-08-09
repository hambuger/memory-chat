package com.github.hambuger.memory.chat.memory.chat.model;

import static com.github.hambuger.memory.chat.memory.other.constants.MemoryChatConstants.REPLY_MESSAGE_FUNCTION_NAME;


/**
 * @author hanjiabao
 * @since 2024/7/23
 */
public enum ChatSceneEnum {

    NORMAL_USER("正常好友沟通", REPLY_MESSAGE_FUNCTION_NAME),
    NORMAL_GROUP("正常群沟通", REPLY_MESSAGE_FUNCTION_NAME),
    SCHEDULE("退避尝试发起对话", REPLY_MESSAGE_FUNCTION_NAME),
    NEWS_SCHEDULE("定时根据热点尝试发起对话", REPLY_MESSAGE_FUNCTION_NAME),
    TASK("延迟任务","sendToOthersMessage"),

    MEMORY_DIMENSION("补全记忆维度","addNewMemory"),

    PLAN("计划生成","generateDayPlan"),

    MEMORY_MERGE("记忆整理", "updateFinishFlag"),

    ;

    private String desc;

    private String endFunctionName;


    ChatSceneEnum(String desc, String endFunctionName) {
        this.desc = desc;
        this.endFunctionName = endFunctionName;
    }

    public String getEndFunctionName() {
        return this.endFunctionName;
    }

}
