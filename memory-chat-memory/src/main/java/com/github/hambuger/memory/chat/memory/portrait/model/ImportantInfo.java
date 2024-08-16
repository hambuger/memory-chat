package com.github.hambuger.memory.chat.memory.portrait.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import lombok.Data;


/**
 * @author hanjiabao
 * @since 2024/8/5
 */
@Data
public class ImportantInfo {

    @JsonPropertyDescription("描述内容的名称")
    @JsonProperty(required = true)
    public String descriptionName;

    @JsonPropertyDescription("描述具体内容")
    @JsonProperty(required = true)
    public String descriptionDetail;
}
