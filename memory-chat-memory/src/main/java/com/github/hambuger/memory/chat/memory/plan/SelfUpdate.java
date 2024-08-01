package com.github.hambuger.memory.chat.memory.plan;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.hambuger.memory.chat.memory.chat.dto.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.functionCall.aop.FunctionCallRegistry;
import com.github.hambuger.memory.chat.memory.util.RedisUtil;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import static com.github.hambuger.memory.chat.memory.constants.MemoryChatConstants.SELF_PORTRAIT_KEY;


@Slf4j
@Component
public class SelfUpdate {

    @Resource
    private RedisUtil redisUtil;


    @Data
    public static class SelfPortrait {

        @JsonPropertyDescription("长期计划")
        @JsonProperty(required = false, defaultValue = "Unknown")
        private String longPlan = "Unknown";

        @JsonPropertyDescription("短期计划")
        @JsonProperty(required = true, defaultValue = "Unknown")
        private String shortTermPlan = "Unknown";

        @JsonPropertyDescription("状态")
        @JsonProperty(required = true, defaultValue = "Unknown")
        private String state = "Unknown";

        @JsonPropertyDescription("喜好")
        @JsonProperty(required = true, defaultValue = "Unknown")
        private String hobby = "Unknown";

        @JsonPropertyDescription("厌恶")
        @JsonProperty(required = true, defaultValue = "Unknown")
        private String disgust = "Unknown";

        @JsonPropertyDescription("正在做")
        @JsonProperty(required = true, defaultValue = "Unknown")
        private String doing = "Unknown";

        @JsonPropertyDescription("其他补充信息,<\"画像维度\":\"维度内容描述\">")
        @JsonProperty(required = false)
        public Map<String, String> otherInfo = new HashMap<>();
    }


    @FunctionCallRegistry(functionDesc = "更新Andrew的自我画像，可与回复消息并行执行", scene = {ChatSceneEnum.NORMAL_USER, ChatSceneEnum.NORMAL_GROUP})
    public boolean updateSelfPortrait(SelfPortrait param) {
        redisUtil.setString(SELF_PORTRAIT_KEY, JSON.toJSONString(param));
        return true;
    }


    public String getSelfPortrait() {
        String portraitStr = redisUtil.getString(SELF_PORTRAIT_KEY);
        if (StringUtils.isBlank(portraitStr)) {
            return null;
        }else {
            SelfPortrait selfPortrait = JSON.parseObject(portraitStr, SelfPortrait.class);
            String formatStr = """
- LongPlan: %s
- ShortTermPlan: %s
- Hobby: %s
- Disgust: %s
- Doing: %s
- State: %s
%s
""";
            StringBuilder otherInfoStr = new StringBuilder();
            if (MapUtils.isNotEmpty(selfPortrait.otherInfo)) {
                for (Map.Entry<String, String> stringEntry : selfPortrait.otherInfo.entrySet()) {
                    otherInfoStr.append("- ").append(stringEntry.getKey()).append(": ").append(stringEntry.getValue()).append("\n");
                }
            }
            return String.format(formatStr, selfPortrait.longPlan, selfPortrait.shortTermPlan, selfPortrait.hobby, selfPortrait.disgust, selfPortrait.doing, selfPortrait.state, otherInfoStr);

        }
    }

}
