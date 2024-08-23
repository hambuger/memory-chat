package com.github.hambuger.memory.chat.memory.memory.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

import com.github.hambuger.memory.chat.memory.other.config.CustomEnumDeserializer;
import lombok.Data;


/**
 * @author hanjiabao
 * @since 2024/8/6
 */
@Data
public class MemoryDimensionInfo {

    @JsonPropertyDescription("对话的重要性分数，0.0-1")
    @JsonProperty(required = true)
    private Double score;

    /**
     * 情感
     */
    @JsonPropertyDescription("对话内容识别出来的情感")
    @JsonProperty(required = true)
    @JSONField(deserializeUsing = CustomEnumDeserializer.class)
    private EmotionEnum emotion;

    /**
     * 总结词语
     */
    @JsonPropertyDescription("对话内容的总结提炼词语")
    @JsonProperty(required = true)
    private List<String> summaryWords;

    @JsonPropertyDescription("大概理由")
    @JsonProperty(required = true)
    private String reason;


    public enum EmotionEnum {
        未知,
        快乐,
        悲伤,
        失落,
        痛苦,
        愤怒,
        害怕,
        焦虑,
        担忧,
        恐慌,
        惊讶,
        厌恶,
        爱,
        亲密,
        羞愧,
        忌妒,
        占有欲,
        成就感,
        平静,
        好奇,
        怀疑,
        犹豫,
        孤独,
        渴望,
        同情,
        厌倦,
        无聊,
        羡慕,
        感谢,
        可怜,
    }
}
