package com.github.hambuger.memory.chat.memory.plan;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.hambuger.memory.chat.memory.chat.SpringAiChat;
import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.other.constants.MemoryChatConstants;
import com.github.hambuger.memory.chat.memory.other.functionCall.aop.FunctionCallRegistry;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.hambuger.memory.chat.memory.other.prompt.PromptFactory;
import com.github.hambuger.memory.chat.memory.other.util.RedisUtil;
import com.github.hambuger.memory.chat.memory.portrait.model.SelfPortrait;

import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


/**
 * @author hamburger
 * @since 2024/8/7
 */
@Component
@Slf4j
public class DayPlanGenerate {

    public static final String DAY_PLAN_KEY = "selfDayPlan";

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private PromptFactory promptFactory;

    @Resource
    private SpringAiChat springAiChat;

    @Resource
    private DelayedTask delayedTask;

    @Data
    public static class HourActivity {

        @JsonPropertyDescription("小时时间点，取值范围为0到23的整数")
        @JsonProperty(required = true)
        private Integer hour;

        @JsonPropertyDescription("做的事情的描述")
        @JsonProperty(required = true)
        private String task;

    }


    @Data
    public static class OneDayActivity {

        @JsonPropertyDescription("每个小时(0-23)活动内容,需要全部的24个小时")
        @JsonProperty(required = true)
        private List<HourActivity> tasks;

    }


    @FunctionCallRegistry(functionDesc = "生成一天24个小时活动内容", scene = {ChatSceneEnum.PLAN, ChatSceneEnum.TASK})
    public Boolean generateDayActivity(OneDayActivity oneDayActivity) {
        if (oneDayActivity == null || CollectionUtils.isEmpty(oneDayActivity.getTasks())) {
            return false;
        }
        Map<String, Object> hourTaskMap = oneDayActivity.getTasks().stream().collect(Collectors.toMap(k -> k.getHour().toString(), HourActivity::getTask));
        redisUtil.reset(DAY_PLAN_KEY);
        redisUtil.saveMap(DAY_PLAN_KEY, hourTaskMap);
        return true;
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void processPlanTasks() {
        String allTask = delayedTask.getAllTask();
        String portraitStr = redisUtil.getString(MemoryChatConstants.SELF_PORTRAIT_KEY);
        SelfPortrait selfPortrait = JSON.parseObject(portraitStr, SelfPortrait.class);
        String planPrompt = promptFactory.getDayPlanPrompt(allTask, selfPortrait.toMarkDown());
        List<OpenAiApi.ChatCompletionMessage> messages = Lists.newArrayList(new OpenAiApi.ChatCompletionMessage(planPrompt, OpenAiApi.ChatCompletionMessage.Role.SYSTEM));
        OpenAiApi.ChatCompletion chatCompletion = springAiChat.generateMsgWithMsgListAndFunctions(messages, false, ChatSceneEnum.PLAN, 1.0f);
        if(chatCompletion != null && chatCompletion.choices() != null){
            OneDayActivity plan = JSON.parseObject(chatCompletion.choices().get(0).message().toolCalls().get(0).function().arguments(), OneDayActivity.class);
            generateDayActivity(plan);
        }
    }

}
