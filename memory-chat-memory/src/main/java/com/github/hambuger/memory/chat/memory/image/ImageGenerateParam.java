package com.github.hambuger.memory.chat.memory.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.io.Serial;
import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;


/**
 * @author hanjiabao
 * @since 2024/9/20
 */
@Data
@AllArgsConstructor
public class ImageGenerateParam implements Serializable {

    @Serial
    private static final long serialVersionUID = -5569301907117136874L;

    @JsonPropertyDescription("Generate picture prompt words")
    @JsonProperty(required = true)
    private String generateText;
}
