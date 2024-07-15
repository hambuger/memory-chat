package com.github.hambuger.memory.chat.memory.chat.dto;

import org.apache.commons.collections4.MapUtils;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;


/**
 * @author hanjiabao
 * @since 2024/7/15
 */
@Data
public class FriendPortrait extends Portrait {

    private String age = "Unknown";

    private String language = "Unknown";

    private String city = "Unknown";

    private String gender = "Unknown";

    private String character = "Unknown";

    private String relationship = "Unknown";

    private String status = "Unknown";

    public String toMarkDown() {
        String formatStr = """
                ## FriendPortrait
                - Name: %s
                - Age: %s
                - Language: %s
                - City: %s
                - Gender: %s
                - Character: %s
                - Relationship: %s
                - Status: %s
                %s
                """;
        StringBuilder otherInfoStr = new StringBuilder();
        if (MapUtils.isNotEmpty(this.otherInfo)) {
            for (Map.Entry<String, String> stringEntry : otherInfo.entrySet()) {
                otherInfoStr.append("- ").append(stringEntry.getKey()).append(": ").append(stringEntry.getValue()).append("\n");
            }
        }
        return String.format(formatStr, this.name, age, language, city, gender, character, relationship, status, otherInfoStr);
    }

}
