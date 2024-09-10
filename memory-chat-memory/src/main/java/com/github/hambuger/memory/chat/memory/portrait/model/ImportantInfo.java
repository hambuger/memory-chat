package com.github.hambuger.memory.chat.memory.portrait.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import lombok.Data;


/**
 * @author hamburger
 * @since 2024/8/5
 */
@Data
public class ImportantInfo {

    @JsonPropertyDescription("The name of the description content")
    @JsonProperty(required = true)
    public String descriptionName;

    @JsonPropertyDescription("Describe the specifics")
    @JsonProperty(required = true)
    public String descriptionDetail;
}
