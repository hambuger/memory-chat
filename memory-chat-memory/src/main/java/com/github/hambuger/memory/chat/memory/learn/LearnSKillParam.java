package com.github.hambuger.memory.chat.memory.learn;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import lombok.Data;


/**
 * @author hanjiabao
 * @since 2024/8/22
 */
@Data
public class LearnSKillParam {

    @JsonPropertyDescription("学习方法的描述")
    @JsonProperty(required = true)
    private String skillDescription;

}
