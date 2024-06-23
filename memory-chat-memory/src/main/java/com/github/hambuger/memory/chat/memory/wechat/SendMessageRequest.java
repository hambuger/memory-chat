package com.github.hambuger.memory.chat.memory.wechat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
public class SendMessageRequest {

    @JsonPropertyDescription("是否需要发送消息")
    @JsonProperty(required = true)
    private boolean needsSending;

    @JsonPropertyDescription("发送的消息列表")
    @JsonProperty()
    private List<SendMessage> sendMessageList;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SendMessage {
        @JsonPropertyDescription("消息内容，如果是图片消息，为图片url")
        @JsonProperty(required = true)
        private String content;
        @JsonPropertyDescription("消息类型，支持 [TEXT, PICTURE]")
        @JsonProperty(required = true)
        private String contentType;
    }
}
