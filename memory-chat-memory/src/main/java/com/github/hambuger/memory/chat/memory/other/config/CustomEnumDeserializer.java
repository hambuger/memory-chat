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
            return (T) getDefaultEnumValue((Class<Enum>) type);
        }
    }

    @Override
    public int getFastMatchToken() {
        return 0;
    }

    private <T extends Enum<T>> T getDefaultEnumValue(Class<T> enumClass) {
        // Return default value if required
        return enumClass.getEnumConstants()[0];
    }
}

