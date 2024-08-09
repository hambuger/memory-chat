package com.github.hambuger.memory.chat.memory.portrait.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import lombok.Data;


/**
 * @author hanjiabao
 * @since 2024/8/5
 */
@Data
public class DimensionInfo {

    @JsonPropertyDescription("维度名称")
    @JsonProperty(required = true)
    public String dimensionName;

    @JsonPropertyDescription("维度描述内容")
    @JsonProperty(required = true)
    public String dimensionDescription;
}
