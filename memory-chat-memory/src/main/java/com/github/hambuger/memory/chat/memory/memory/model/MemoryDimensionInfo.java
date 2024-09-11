package com.github.hambuger.memory.chat.memory.memory.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

import com.github.hambuger.memory.chat.memory.other.config.CustomEnumDeserializer;
import lombok.Data;


/**
 * @author hamburger
 * @since 2024/8/6
 */
@Data
public class MemoryDimensionInfo {

    @JsonPropertyDescription("conversation importance score，0.0-1")
    @JsonProperty(required = true)
    private Double score;

    /**
     * Emotion
     */
    @JsonPropertyDescription("emotions recognized from conversation content")
    @JsonProperty(required = true)
    @JSONField(deserializeUsing = CustomEnumDeserializer.class)
    private EmotionEnum emotion;

    /**
     * SUMMARY WORDS
     */
    @JsonPropertyDescription("summarize and extract words from the conversation")
    @JsonProperty(required = true)
    private List<String> summaryWords;

    @JsonPropertyDescription("reason")
    @JsonProperty(required = true)
    private String reason;


    public enum EmotionEnum {
        Unknown,
        Happiness,
        Sadness,
        Loss,
        Pain,
        Anger,
        Fear,
        Anxiety,
        Worry,
        Panic,
        Surprise,
        Disgust,
        Love,
        Intimacy,
        Shame,
        Jealousy,
        Possessiveness,
        Accomplishment,
        Calmness,
        Curiosity,
        Doubt,
        Hesitation,
        Loneliness,
        Longing,
        Compassion,
        Tiredness,
        Boredom,
        Envy,
        Gratitude,
        Pity;
    }
}
