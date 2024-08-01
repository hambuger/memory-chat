package com.github.hambuger.memory.chat.memory.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.MapUtils;

import java.util.HashMap;
import java.util.Map;


/**
 * @author hanjiabao
 * @since 2024/7/15
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupPortrait {

    @JsonPropertyDescription("群名称")
    @JsonProperty(required = true, defaultValue = "Unknown")
    public String name;

    @JsonPropertyDescription("群的其他补充信息,<\"画像维度\":\"维度内容描述\">")
    @JsonProperty(required = false)
    public Map<String, String> otherInfo = new HashMap<>();

    public String toMarkDown() {
        StringBuilder otherInfoStr = new StringBuilder();
        if (MapUtils.isNotEmpty(this.otherInfo)) {
            for (Map.Entry<String, String> stringEntry : otherInfo.entrySet()) {
                otherInfoStr.append("- ").append(stringEntry.getKey()).append(": ").append(stringEntry.getValue()).append("\n");
            }
        }
        String formatStr = """
                ## GroupPortrait
                - Name: %s
                %s
                """;
        return String.format(formatStr, this.name, otherInfoStr);
    }
}
