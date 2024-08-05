package com.github.hambuger.memory.chat.memory.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;


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

    @JsonPropertyDescription("群的其他维度补充信息")
    @JsonProperty(required = false)
    public List<DimensionInfo> otherInfo = new ArrayList<>();

    public String toMarkDown() {
        StringBuilder otherInfoStr = new StringBuilder();
        if (CollectionUtils.isNotEmpty(this.otherInfo)) {
            for (DimensionInfo info : otherInfo) {
                otherInfoStr.append("- ").append(info.getDimensionName()).append(": ").append(info.getDimensionDescription()).append("\n");
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
