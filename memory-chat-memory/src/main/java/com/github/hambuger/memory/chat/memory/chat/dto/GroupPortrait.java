package com.github.hambuger.memory.chat.memory.chat.dto;

import lombok.Data;


/**
 * @author hanjiabao
 * @since 2024/7/15
 */
@Data
public class GroupPortrait extends Portrait {


    public String toMarkDown() {
        String formatStr = """
                ## GroupPortrait
                - Name: %s
                %s
                """;
        return String.format(formatStr, this.name);
    }
}
