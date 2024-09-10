package com.github.hambuger.memory.chat.memory.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @author hamburger
 * @since 2024/6/23
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendMessage {

    @JsonPropertyDescription("The content of the message, if it is an image message, is the image URL")
    @JsonProperty(required = true)
    private String messageContent;

    @JsonPropertyDescription("Message type, supports [TEXT, PICTURE]")
    @JsonProperty(required = true)
    private String messageContentType;
}
