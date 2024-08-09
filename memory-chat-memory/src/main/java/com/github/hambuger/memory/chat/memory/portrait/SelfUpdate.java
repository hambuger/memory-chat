package com.github.hambuger.memory.chat.memory.portrait;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.plan.DayPlanGenerate;
import com.github.hambuger.memory.chat.memory.portrait.model.DimensionInfo;
import com.github.hambuger.memory.chat.memory.other.functionCall.aop.FunctionCallRegistry;
import com.github.hambuger.memory.chat.memory.memory.model.MemoryDimensionInfo;
import com.github.hambuger.memory.chat.memory.other.config.CustomEnumDeserializer;
import com.github.hambuger.memory.chat.memory.other.util.RedisUtil;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import static com.github.hambuger.memory.chat.memory.other.constants.MemoryChatConstants.SELF_PORTRAIT_KEY;


@Slf4j
@Component
public class SelfUpdate {

    @Resource
    private RedisUtil redisUtil;


    @Data
    public static class SelfPortrait {

//        @JsonPropertyDescription("长期计划")
//        @JsonProperty(required = false, defaultValue = "Unknown")
//        private String longPlan = "Unknown";
//
//        @JsonPropertyDescription("短期计划")
//        @JsonProperty(required = true, defaultValue = "Unknown")
//        private String shortTermPlan = "Unknown";

        @JsonPropertyDescription("情绪")
        @JsonProperty(required = true)
        @JSONField(deserializeUsing = CustomEnumDeserializer.class)
        private MemoryDimensionInfo.EmotionEnum emotion;

        @JsonPropertyDescription("状态")
        @JsonProperty(required = true, defaultValue = "Unknown")
        private String state = "Unknown";

        @JsonPropertyDescription("喜好")
        @JsonProperty(required = true, defaultValue = "Unknown")
        private String hobby = "Unknown";

        @JsonPropertyDescription("厌恶")
        @JsonProperty(required = true, defaultValue = "Unknown")
        private String disgust = "Unknown";
//
//        @JsonPropertyDescription("正在做")
//        @JsonProperty(required = true, defaultValue = "Unknown")
//        private String doing = "Unknown";

        @JsonPropertyDescription("其他维度补充信息")
        @JsonProperty(required = false)
        public List<DimensionInfo> otherInfo = new ArrayList<>();

    }


    @FunctionCallRegistry(functionDesc = "更新Andrew的自我画像，可与回复消息并行执行", scene = {ChatSceneEnum.NORMAL_USER, ChatSceneEnum.NORMAL_GROUP})
    public boolean updateSelfPortrait(SelfPortrait param) {
        redisUtil.setString(SELF_PORTRAIT_KEY, JSON.toJSONString(param));
        return true;
    }


    public String getSelfPortrait() {
        String portraitStr = redisUtil.getString(SELF_PORTRAIT_KEY);
        Map<Object, Object> hourPlanMap = redisUtil.getMap(DayPlanGenerate.DAY_PLAN_KEY);
        int nowHour = DateUtil.thisHour(true);
        String hourTask = Optional.ofNullable(hourPlanMap).map(map -> {
            StringBuilder info = new StringBuilder(map.get(Integer.toString(nowHour)).toString());
            info.append("(");
            info.append("已进行了");
            int minute = DateUtil.minute(new Date());
            info.append(minute);
            info.append("分钟, 还剩下");
            info.append(60 - minute);
            info.append("分钟)");
            return info.toString();
        }).orElse("Unknown");
        if (StringUtils.isBlank(portraitStr)) {
            return null;
        }else {
            SelfPortrait selfPortrait = JSON.parseObject(portraitStr, SelfPortrait.class);
//            - LongPlan: %s
//            - ShortTermPlan: %s
            String formatStr = """
- emotion: %s
- Hobby: %s
- Disgust: %s
- Doing: %s
- State: %s
%s
""";
            StringBuilder otherInfoStr = new StringBuilder();
            if (CollectionUtils.isNotEmpty(selfPortrait.otherInfo)) {
                for (DimensionInfo info : selfPortrait.otherInfo) {
                    otherInfoStr.append("- ").append(info.getDimensionName()).append(": ").append(info.getDimensionDescription()).append("\n");
                }
            }
            return String.format(formatStr,
//                    selfPortrait.longPlan, selfPortrait.shortTermPlan,
                    Optional.ofNullable(selfPortrait.emotion).map(Enum::name).orElse("Unknown"), selfPortrait.hobby, selfPortrait.disgust, hourTask, selfPortrait.state, otherInfoStr);

        }
    }

}
