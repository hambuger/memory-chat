package com.github.hambuger.memory.chat.memory.portrait.model;

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

    @JsonPropertyDescription("群的其他重要补充信息")
    @JsonProperty(required = false)
    public List<ImportantInfo> otherImportantInfo = new ArrayList<>();

    public String toMarkDown() {
        StringBuilder otherInfoStr = new StringBuilder();
        if (CollectionUtils.isNotEmpty(this.otherImportantInfo)) {
            for (ImportantInfo info : otherImportantInfo) {
                otherInfoStr.append("- ").append(info.getDescriptionName()).append(": ").append(info.getDescriptionDetail()).append("\n");
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
