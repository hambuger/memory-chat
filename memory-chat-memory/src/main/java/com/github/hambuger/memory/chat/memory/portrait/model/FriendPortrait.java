package com.github.hambuger.memory.chat.memory.portrait.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @author hamburger
 * @since 2024/7/15
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FriendPortrait extends BasePortrait{

    @JsonPropertyDescription("name")
    @JsonProperty(required = true, defaultValue = "Unknown")
    public String name;

    @JsonPropertyDescription("nickName")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String nickName = "Unknown";

    @JsonPropertyDescription("Relationship with Andrew")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String relationship = "Unknown";

    @JsonPropertyDescription("doing")
    @JsonProperty(required = true, defaultValue = "Unknown")
    private String doing = "Unknown";

    @JsonPropertyDescription("and Andrew's familiarity")
    @JsonProperty(required = true)
    private String familiarityDegree;


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
