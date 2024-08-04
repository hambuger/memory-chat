package com.github.hambuger.memory.chat.memory.wechat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendOthersMessageRequest extends SendMessageRequest{

    @JsonPropertyDescription("消息接收人")
    @JsonProperty(required = true)
    private String receiveName;
}
