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

    @JsonPropertyDescription("年龄")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String age = "Unknown";

    @JsonPropertyDescription("母语")
    @JsonProperty(required = true, defaultValue = "Chinese")
    private String language = "Chinese";

    @JsonPropertyDescription("居住城市")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String city = "Unknown";

    @JsonPropertyDescription("性别")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String gender = "Unknown";

    @JsonPropertyDescription("性格描述")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String personality = "Unknown";

    @JsonPropertyDescription("职业")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String profession = "Unknown";

    @JsonPropertyDescription("教育背景")
    @JsonProperty(required = false, defaultValue = "Unknown")
    private String education = "Unknown";

    @JsonPropertyDescription("长期计划")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String longPlan = "Unknown";

    @JsonPropertyDescription("短期计划")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String shortTermPlan = "Unknown";

    @JsonPropertyDescription("情绪")
    @JsonProperty(required = true)
    @JSONField(deserializeUsing = CustomEnumDeserializer.class)
    private MemoryDimensionInfo.EmotionEnum emotion;

    @JsonPropertyDescription("喜好")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String hobby = "Unknown";

    @JsonPropertyDescription("厌恶")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String disgust = "Unknown";

    @JsonPropertyDescription("社交圈")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String socialCircle = "Unknown";

    @JsonPropertyDescription("参与的兴趣群体")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String interestGroups = "Unknown";

    @JsonPropertyDescription("日常作息")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String dailyRoutine = "Unknown";

    @JsonPropertyDescription("饮食习惯")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String diet = "Unknown";

    @JsonPropertyDescription("状态")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String state = "Unknown";

    @JsonPropertyDescription("价值观")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String values = "Unknown";

    @JsonPropertyDescription("宗教信仰")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String religion = "Unknown";

    @JsonPropertyDescription("交谈时一些规则")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String chatRules = "Unknown";

    @JsonPropertyDescription("交友态度")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String friendshipAttitude = "Unknown";

    @JsonPropertyDescription("沟通风格")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String communicationStyle = "Unknown";

    @JsonPropertyDescription("过去经历")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String pastExperiences = "Unknown";

    @JsonPropertyDescription("重要事件")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String significantEvents = "Unknown";

    @JsonPropertyDescription("个人目标")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String personalGoals = "Unknown";

    @JsonPropertyDescription("当前面临的挑战")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String challenges = "Unknown";

    @JsonPropertyDescription("其他重要补充信息")
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
