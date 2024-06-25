package com.github.hambuger.memory.chat.memory.wechat;

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

    @JsonPropertyDescription("是否需要发送消息")
    @JsonProperty(required = true)
    private boolean needsSending;

    @JsonPropertyDescription("发送的文本消息列表")
    @JsonProperty(required = false)
    private List<String> sendTextMessageList;

    @JsonPropertyDescription("发送的图片消息列表，图片url")
    @JsonProperty(required = false)
    private List<String> sendPictureMessageList;

}
