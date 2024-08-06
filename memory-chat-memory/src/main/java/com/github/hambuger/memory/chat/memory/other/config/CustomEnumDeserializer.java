package com.github.hambuger.memory.chat.memory.other.config;

import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;

import java.lang.reflect.Type;

public class CustomEnumDeserializer implements ObjectDeserializer {

    @Override
    public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        String value = parser.getLexer().stringVal();
        try {
            return (T) Enum.valueOf((Class<Enum>) type, value);
        } catch (IllegalArgumentException e) {
            // 返回一个默认值或其他处理方式
            return (T) getDefaultEnumValue((Class<Enum>) type);
        }
    }

    @Override
    public int getFastMatchToken() {
        return 0;
    }

    private <T extends Enum<T>> T getDefaultEnumValue(Class<T> enumClass) {
        // 根据需求返回默认值
        return enumClass.getEnumConstants()[0]; // 返回第一个枚举值作为默认值
    }
}

