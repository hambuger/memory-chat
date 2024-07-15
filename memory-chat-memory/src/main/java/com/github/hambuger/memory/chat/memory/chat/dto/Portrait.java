package com.github.hambuger.memory.chat.memory.chat.dto;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;


/**
 * @author hanjiabao
 * @since 2024/7/15
 */
@Data
public class Portrait {

    public String name;

    public Map<String, String> otherInfo = new HashMap<>();

}
