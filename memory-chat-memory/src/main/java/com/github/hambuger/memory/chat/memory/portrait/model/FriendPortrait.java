package com.github.hambuger.memory.chat.memory.portrait.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @author hanjiabao
 * @since 2024/7/15
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FriendPortrait extends BasePortrait{

    @JsonPropertyDescription("名称")
    @JsonProperty(required = true, defaultValue = "Unknown")
    public String name;

    @JsonPropertyDescription("昵称")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String nickName = "Unknown";

    @JsonPropertyDescription("和Andrew的关系")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String relationship = "Unknown";

    @JsonPropertyDescription("正在做")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String doing = "Unknown";


    public String toMarkDown() {
        String formatStr = """
## FriendPortrait
%s
""";
        return String.format(formatStr, super.toMarkDown());
    }

    @Override
    public String toString() {
        return super.toString();
    }

}
