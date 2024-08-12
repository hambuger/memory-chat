package com.github.hambuger.memory.chat.memory.chat.model;

import com.google.common.base.Objects;


/**
 * @author hamburger
 * @since 2024/6/18
 */
public enum ContentTypeEnum {

    TEXT(1, "TEXT", "文本"),
    PICTURE(3, "PICTURE", "图片"),
    AUDIO(34, "AUDIO", "语音"),
    VIDEO(43, "VIDEO", "视频"),
    EMOJI(47, "EMOJI", "表情"),
    NOTE(10000, "NOTE", "提醒"),
    APP(49, "APP", "文件"),
    ;

    private Integer wxType;

    private String type;

    private String desc;


    public String getType() {
        return type;
    }

    public Integer getMsgType() {
        return wxType;
    }


    ContentTypeEnum(Integer wxType, String type, String desc) {
        this.wxType = wxType;
        this.type = type;
        this.desc = desc;
    }


    public static ContentTypeEnum getByWxType(Integer type) {
        for (ContentTypeEnum typeEnum : ContentTypeEnum.values()) {
            if (Objects.equal(typeEnum.wxType, type)) {
                return typeEnum;
            }
        }
        return null;
    }

    public static ContentTypeEnum getByType(String type) {
        for (ContentTypeEnum typeEnum : ContentTypeEnum.values()) {
            if (Objects.equal(typeEnum.type, type)) {
                return typeEnum;
            }
        }
        return null;
    }

}
