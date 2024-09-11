package com.github.hambuger.memory.chat.memory.chat.model;

import static com.github.hambuger.memory.chat.memory.other.constants.MemoryChatConstants.REPLY_MESSAGE_FUNCTION_NAME;


/**
 * @author hamburger
 * @since 2024/7/23
 */
public enum ChatSceneEnum {

    NORMAL_USER("Normal friend communication", REPLY_MESSAGE_FUNCTION_NAME),

    NORMAL_GROUP("Normal group communication", REPLY_MESSAGE_FUNCTION_NAME),
    SCHEDULE("Avoid trying to initiate a conversation", REPLY_MESSAGE_FUNCTION_NAME),
    NEWS_SCHEDULE("Timed attempts to initiate a conversation based on hot spots", REPLY_MESSAGE_FUNCTION_NAME),
    TASK("Delayed tasks","updateFinishFlag"),

    MEMORY_DIMENSION("Complete memory dimensions","addNewMemory"),

    PLAN("Plan generation","updateFinishFlag"),

    MEMORY_MERGE("Memory organization","updateFinishFlag"),

    LEARN_SKILL("Skill learning","learnSkillProcess"),

    LEARN_JUDGE("Do you want to learn","updateFinishFlag"),

    LEARN_FUNCTION("Code learning", "addNewFunction"),

    ROLE_CHANGE("Role change", "updateFinishFlag"),

    UPDATE_FRIEND_PORTRAIT("Friend portrait change", "updateFinishFlag"),

    UPDATE_SELF_PORTRAIT("Andrew portrait change", "updateFinishFlag"),

    RULE_CHANGE("Rule change", "updateFinishFlag"),
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
