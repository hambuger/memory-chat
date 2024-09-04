package com.github.hambuger.memory.chat.memory.emoji;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * @author hanjiabao
 * @since 2024/9/4
 */
@Slf4j
@Component
public class SelfEmoji {

    @Data
    @NoArgsConstructor
    public static class EmojiExtraWord {

        @JsonPropertyDescription("表情风格")
        @JsonProperty(required = true)
        private String emojiStyle;

        @JsonPropertyDescription("表情类别")
        @JsonProperty(required = true)
        private String emojiCategory;

        @JsonPropertyDescription("表情标题")
        @JsonProperty(required = true)
        private String emojiTitle;

        @JsonPropertyDescription("情感标签")
        @JsonProperty(required = true)
        private String emojiEmotionTag;

        @JsonPropertyDescription("视觉特征描述")
        private String emojiVisualFeatureDescription;

        @JsonPropertyDescription("使用场景或语境")
        @JsonProperty(required = true)
        private String emojiUsageContext;
    }



}
