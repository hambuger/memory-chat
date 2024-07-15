package com.github.hambuger.memory.chat.memory.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.MapUtils;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;


/**
 * @author hanjiabao
 * @since 2024/7/15
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FriendPortrait {

    @JsonPropertyDescription("好友名称")
    @JsonProperty(required = true, defaultValue = "Unknown")
    public String name;

    @JsonPropertyDescription("好友年龄")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String age = "Unknown";

    @JsonPropertyDescription("好友的母语")
    @JsonProperty(required = true, defaultValue = "Chinese")
    private String language = "Chinese";

    @JsonPropertyDescription("好友居住城市")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String city = "Unknown";

    @JsonPropertyDescription("好友性别")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String gender = "Unknown";

    @JsonPropertyDescription("好友性格描述")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String character = "Unknown";

    @JsonPropertyDescription("和Andrew的关系")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String relationship = "Unknown";

    @JsonPropertyDescription("好友状态")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String status = "Unknown";


    @JsonPropertyDescription("好友的其他补充信息")
    @JsonProperty(required = false)
    public Map<String, String> otherInfo = new HashMap<>();

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
