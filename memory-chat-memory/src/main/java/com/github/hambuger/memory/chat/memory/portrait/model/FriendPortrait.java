package com.github.hambuger.memory.chat.memory.portrait.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import com.github.hambuger.memory.chat.memory.memory.model.MemoryDimensionInfo;
import com.github.hambuger.memory.chat.memory.other.config.CustomEnumDeserializer;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


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

    @JsonPropertyDescription("好友情绪")
    @JsonProperty(required = true)
    @JSONField(deserializeUsing = CustomEnumDeserializer.class)
    private MemoryDimensionInfo.EmotionEnum emotion;

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


    @JsonPropertyDescription("好友的其他维度补充信息")
    @JsonProperty(required = false)
    public List<DimensionInfo> otherInfo = new ArrayList<>();


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
- emotion: %s
- Hobby: %s
- Disgust: %s
- Doing: %s
- State: %s
%s
""";
        StringBuilder otherInfoStr = new StringBuilder();
        if (CollectionUtils.isNotEmpty(this.otherInfo)) {
            for (DimensionInfo info : otherInfo) {
                otherInfoStr.append("- ").append(info.getDimensionName()).append(": ").append(info.getDimensionDescription()).append("\n");
            }
        }
        return String.format(formatStr, this.name, age, language, city, gender, personality, relationship, longPlan, shortTermPlan, Optional.ofNullable(emotion).map(Enum::name).orElse("Unknown"), hobby, disgust, doing, state, otherInfoStr);
    }

}
