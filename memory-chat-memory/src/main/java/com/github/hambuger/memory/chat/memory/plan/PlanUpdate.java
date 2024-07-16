package com.github.hambuger.memory.chat.memory.plan;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.hambuger.memory.chat.memory.functionCall.aop.FunctionCallRegistry;
import com.github.hambuger.memory.chat.memory.util.RedisUtil;

import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import static com.github.hambuger.memory.chat.memory.constants.MemoryChatConstants.SELF_STATUS_KEY;


@Slf4j
@Component
public class PlanUpdate {

    @Resource
    private RedisUtil redisUtil;


    @Data
    public static class SelfPlanAndStatus {

        @JsonPropertyDescription("计划和状态描述")
        @JsonProperty(required = true)
        private String planAndStatusDesc;
    }


    @FunctionCallRegistry(functionDesc = "更新Andrew的计划或者状态")
    public boolean updateSelfPlanAndStatus(SelfPlanAndStatus param) {
        redisUtil.setString(SELF_STATUS_KEY, param.getPlanAndStatusDesc());
        return true;
    }

}
