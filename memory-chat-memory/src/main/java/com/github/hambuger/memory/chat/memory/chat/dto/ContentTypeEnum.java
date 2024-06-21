package com.github.hambuger.memory.chat.memory.chat.dto;

/**
 * @author hamburger
 * @since 2024/6/18
 */
public enum ContentTypeEnum {

    TEXT("TEXT", "文本"),
    PICTURE("PICTURE", "图片"),
    AUDIO("AUDIO", "语音"),
    NOTE("NOTE", "提醒"),
    ;

    private String type;

    private String desc;


    public String getType() {
        return type;
    }


    ContentTypeEnum(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

}
