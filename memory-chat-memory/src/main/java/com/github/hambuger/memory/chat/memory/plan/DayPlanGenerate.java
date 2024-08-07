package com.github.hambuger.memory.chat.memory.plan;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.hambuger.memory.chat.memory.functionCall.aop.FunctionCallRegistry;

import java.util.List;

import lombok.Data;


/**
 * @author hanjiabao
 * @since 2024/8/7
 */
public class DayPlanGenerate {

    @Data
    public static class HourPlan {

        @JsonPropertyDescription("小时时间点，取值范围为0到23的整数")
        @JsonProperty(required = true)
        private Integer hour;

        @JsonPropertyDescription("要做的事情的描述")
        @JsonProperty(required = true)
        private String task;

    }


    @Data
    public static class OneDayPlan {

        @JsonPropertyDescription("每个小时(0-23)计划list")
        @JsonProperty(required = true)
        private List<HourPlan> tasks;

    }


    @FunctionCallRegistry(functionDesc = "生成今日每个小时(0-23)计划list", scene = {})
    public boolean generateDayPlan(OneDayPlan oneDayPlan) {
        return true;

    }

}
