package com.github.hambuger.memory.chat.memory.chat.model;

import com.google.common.base.Objects;


/**
 * @author hamburger
 * @since 2024/6/18
 */
public enum ContentTypeEnum {

    TEXT(1, "TEXT"),
    PICTURE(3, "PICTURE"),
    AUDIO(34, "AUDIO"),
    VIDEO(43, "VIDEO"),
    EMOJI(47, "EMOJI"),
    NOTE(10000, "NOTE"),
    APP(49, "APP"),
    ;

    private Integer wxType;

    private String type;


    public String getType() {
        return type;
    }

    public Integer getMsgType() {
        return wxType;
    }


    ContentTypeEnum(Integer wxType, String type) {
        this.wxType = wxType;
        this.type = type;
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
