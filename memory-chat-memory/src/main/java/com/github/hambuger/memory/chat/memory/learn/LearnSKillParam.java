package com.github.hambuger.memory.chat.memory.learn;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import lombok.Data;


/**
 * @author hamburger
 * @since 2024/8/22
 */
@Data
public class LearnSKillParam {

    @JsonPropertyDescription("Description of learning skill")
    @JsonProperty(required = true)
    private String skillDescription;

}
