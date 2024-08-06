package com.github.hambuger.memory.chat.memory.memory.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

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
        满足,
        幸福,
        愉悦,
        喜悦,
        悲伤,
        失落,
        忧伤,
        沮丧,
        痛苦,
        愤怒,
        愤慨,
        恼怒,
        愤恨,
        激怒,
        恐惧,
        害怕,
        焦虑,
        担忧,
        恐慌,
        惊讶,
        震惊,
        意外,
        惊奇,
        厌恶,
        反感,
        憎恶,
        嫌弃,
        爱,
        亲密,
        依恋,
        欣赏,
        忠诚,
        羞耻,
        羞愧,
        内疚,
        耻辱,
        自责,
        嫉妒,
        忌妒,
        占有欲,
        敌意,
        悔恨,
        负疚感,
        骄傲,
        自豪,
        自尊,
        成就感,
        失望,
        挫折,
        平静,
        好奇,
        宁静,
        安详,
        放松,
        怀疑,
        疑惑,
        不确定,
        犹豫,
        孤独,
        孤单,
        寂寞,
        被疏远感,
        知足,
        安逸,
        渴望,
        欲望,
        期望,
        憧憬,
        焦躁,
        不安,
        紧张,
        希望,
        期待,
        乐观,
        受挫,
        懊恼,
        安慰,
        慰藉,
        安抚,
        平复,
        极度惊讶,
        无法置信,
        震动,
        同情,
        怜悯,
        同理心,
        体谅,
        厌倦,
        无聊,
        倦怠,
        愧疚,
        懊悔,
        烦躁,
        不耐烦,
        羡慕,
        不甘心,
        惊恐,
        惊慌失措,
        困惑,
        迷茫,
        不解,
        感激,
        感谢,
        报答,
        可怜,
        敬畏,
        钦佩,
        感到渺小;
    }
}
