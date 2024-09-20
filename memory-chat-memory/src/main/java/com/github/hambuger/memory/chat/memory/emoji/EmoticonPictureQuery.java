package com.github.hambuger.memory.chat.memory.emoji;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.io.Serial;
import java.io.Serializable;

import lombok.Data;


/**
 * @author hanjiabao
 * @since 2024/9/20
 */
@Data
public class EmoticonPictureQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = -581056212259596149L;

    @JsonPropertyDescription("Emoticon image search text")
    @JsonProperty(required = true)
    private String emoticonPictureQueryWord;

}
