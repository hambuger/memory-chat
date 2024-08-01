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
    private String personality = "Unknown";

    @JsonPropertyDescription("和Andrew的关系")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String relationship = "Unknown";

    @JsonPropertyDescription("好友长期计划")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String longPlan = "Unknown";

    @JsonPropertyDescription("好友短期计划")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String shortTermPlan = "Unknown";

    @JsonPropertyDescription("好友喜好")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String hobby = "Unknown";

    @JsonPropertyDescription("好友厌恶")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String disgust = "Unknown";

    @JsonPropertyDescription("好友正在做")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String doing = "Unknown";

    @JsonPropertyDescription("好友状态")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String state = "Unknown";


    @JsonPropertyDescription("好友的其他补充信息,<\"画像维度\":\"维度内容描述\">")
    @JsonProperty(required = false)
    public Map<String, String> otherInfo = new HashMap<>();

    public String toMarkDown() {
        String formatStr = """
                ## FriendPortrait
                - Name: %s
                - Age: %s
                - NativeLanguage: %s
                - City: %s
                - Gender: %s
                - Personality: %s
                - Relationship: %s
                - LongPlan: %s
                - ShortTermPlan: %s
                - Hobby: %s
                - Disgust: %s
                - Doing: %s
                - State: %s
                %s
                """;
        StringBuilder otherInfoStr = new StringBuilder();
        if (MapUtils.isNotEmpty(this.otherInfo)) {
            for (Map.Entry<String, String> stringEntry : otherInfo.entrySet()) {
                otherInfoStr.append("- ").append(stringEntry.getKey()).append(": ").append(stringEntry.getValue()).append("\n");
            }
        }
        return String.format(formatStr, this.name, age, language, city, gender, personality, relationship, longPlan, shortTermPlan, hobby, disgust, doing, state, otherInfoStr);
    }

}
