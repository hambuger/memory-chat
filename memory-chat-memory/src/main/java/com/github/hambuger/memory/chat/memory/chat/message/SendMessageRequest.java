package com.github.hambuger.memory.chat.memory.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendMessageRequest {

    @JsonPropertyDescription("Whether you need to send a message")
    @JsonProperty(required = true)
    private boolean needsSending;

    @JsonPropertyDescription("A list of text messages sent")
    @JsonProperty(required = false)
    private List<String> sendTextMessageList;

    @JsonPropertyDescription("List of image messages sent, image URL")
    @JsonProperty(required = false)
    private List<String> sendPictureMessageList;

    @JsonPropertyDescription("The URL of the sent emoji image must be obtained by searching for it through the searchEmoticonPhoto tool")
    @JsonProperty(required = false)
    private List<String> emoticonPhotoUrlList;

}
