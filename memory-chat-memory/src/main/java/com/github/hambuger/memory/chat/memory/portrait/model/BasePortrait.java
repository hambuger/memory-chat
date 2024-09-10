package com.github.hambuger.memory.chat.memory.portrait.model;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.hambuger.memory.chat.memory.memory.model.MemoryDimensionInfo;
import com.github.hambuger.memory.chat.memory.other.config.CustomEnumDeserializer;

import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.hutool.core.map.MapUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @author hamburger
 * @since 2024/8/23
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BasePortrait {

    @JsonPropertyDescription("age")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String age = "Unknown";

    @JsonPropertyDescription("native language")
    @JsonProperty(required = true, defaultValue = "Chinese")
    private String language = "Chinese";

    @JsonPropertyDescription("City of residence")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String city = "Unknown";

    @JsonPropertyDescription("gender")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String gender = "Unknown";

    @JsonPropertyDescription("Personality description")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String personality = "Unknown";

    @JsonPropertyDescription("profession")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String profession = "Unknown";

    @JsonPropertyDescription("education")
    @JsonProperty(required = false, defaultValue = "Unknown")
    private String education = "Unknown";

    @JsonPropertyDescription("long term planning")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String longPlan = "Unknown";

    @JsonPropertyDescription("short term plans")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String shortTermPlan = "Unknown";

    @JsonPropertyDescription("emotion")
    @JsonProperty(required = true)
    @JSONField(deserializeUsing = CustomEnumDeserializer.class)
    private MemoryDimensionInfo.EmotionEnum emotion;

    @JsonPropertyDescription("hobby")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String hobby = "Unknown";

    @JsonPropertyDescription("disgust")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String disgust = "Unknown";

    @JsonPropertyDescription("social circles")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String socialCircle = "Unknown";

    @JsonPropertyDescription("Participating interest groups")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String interestGroups = "Unknown";

    @JsonPropertyDescription("Daily routine")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String dailyRoutine = "Unknown";

    @JsonPropertyDescription("Eating habits")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String diet = "Unknown";

    @JsonPropertyDescription("state")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String state = "Unknown";

    @JsonPropertyDescription("values")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String values = "Unknown";

    @JsonPropertyDescription("Religious beliefs")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String religion = "Unknown";

    @JsonPropertyDescription("Some rules when talking")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String chatRules = "Unknown";

    @JsonPropertyDescription("Attitude towards making friends")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String friendshipAttitude = "Unknown";

    @JsonPropertyDescription("Communication style")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String communicationStyle = "Unknown";

    @JsonPropertyDescription("Past experiences")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String pastExperiences = "Unknown";

    @JsonPropertyDescription("Important Events")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String significantEvents = "Unknown";

    @JsonPropertyDescription("Personal goals")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String personalGoals = "Unknown";

    @JsonPropertyDescription("Current challenges")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String challenges = "Unknown";

    @JsonPropertyDescription("Other important additional information")
    @JsonProperty(required = false)
    public List<ImportantInfo> otherImportantInfo = new ArrayList<>();


    public String toMarkDown() {
        Map<String, Object> map = JSONObject.parseObject(this.toString()).getInnerMap();
        map.remove("otherImportantInfo");
        if (MapUtil.isEmpty(map)) {
            return "";
        }else {
            StringBuilder stringBuilder = new StringBuilder();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                stringBuilder.append("- ").append(entry.getKey()).append(": ").append(JSON.toJSONString(entry.getValue())).append("\n");
            }
            if (CollectionUtils.isNotEmpty(this.otherImportantInfo)) {
                for (ImportantInfo info : otherImportantInfo) {
                    stringBuilder.append("- ").append(info.getDescriptionName()).append(": ").append(info.getDescriptionDetail()).append("\n");
                }
            }
            return stringBuilder.toString();
        }
    }


    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

}
