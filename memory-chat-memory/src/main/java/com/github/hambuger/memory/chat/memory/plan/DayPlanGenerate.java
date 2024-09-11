package com.github.hambuger.memory.chat.memory.plan;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.hambuger.memory.chat.memory.chat.SpringAiChat;
import com.github.hambuger.memory.chat.memory.chat.model.ChatMember;
import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.other.functionCall.aop.FunctionCallRegistry;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.hambuger.memory.chat.memory.other.prompt.PromptFactory;
import com.github.hambuger.memory.chat.memory.other.util.RedisUtil;
import com.github.hambuger.memory.chat.memory.other.util.UserInfoUtil;
import com.github.hambuger.memory.chat.memory.portrait.SelfUpdate;
import com.github.hambuger.memory.chat.memory.portrait.model.SelfPortrait;

import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.util.StringUtil;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static com.github.hambuger.memory.chat.memory.other.constants.MemoryChatConstants.SELF_PORTRAIT_KEY;


/**
 * @author hamburger
 * @since 2024/8/7
 */
@Component
@Slf4j
public class DayPlanGenerate {

    public static final String DAY_PLAN_KEY = "selfDayPlan";

    public static final String CUSTOM_DAY_PLAN_KEY = "%s:selfDayPlan";

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private PromptFactory promptFactory;

    @Resource
    private SpringAiChat springAiChat;

    @Resource
    private DelayedTask delayedTask;

    public static String getCustomDayPlanKey() {
        return String.format(CUSTOM_DAY_PLAN_KEY, UserInfoUtil.getUser());
    }

    @Data
    public static class HourActivity {

        @JsonPropertyDescription("hour time point, an integer ranging from 0 to 23")
        @JsonProperty(required = true)
        private Integer hour;

        @JsonPropertyDescription("Description of what was done")
        @JsonProperty(required = true)
        private String task;

    }


    @Data
    public static class OneDayActivity {

        @JsonPropertyDescription("Each hour (0-23) activity content requires all 24 hours")
        @JsonProperty(required = true)
        private List<HourActivity> tasks;

    }


    @FunctionCallRegistry(functionDesc = "Generate activity content 24 hours a day", scene = {ChatSceneEnum.PLAN})
    public Boolean generateDayActivity(OneDayActivity oneDayActivity) {
        if (oneDayActivity == null || CollectionUtils.isEmpty(oneDayActivity.getTasks())) {
            return false;
        }
        Map<String, Object> hourTaskMap = oneDayActivity.getTasks().stream().collect(Collectors.toMap(k -> k.getHour().toString(), HourActivity::getTask));
        String planKey;
        if (StringUtil.isNotBlank(UserInfoUtil.getUser())) {
            planKey = getCustomDayPlanKey();
        } else {
            return true;
        }
        redisUtil.reset(planKey);
        redisUtil.saveMap(planKey, hourTaskMap);
        return true;
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void processPlanTasks() {
        List<Object> allMembers = redisUtil.getAllMembers();
        if (CollectionUtils.isEmpty(allMembers)) {
            return;
        }
        allMembers.parallelStream().forEach(member -> {
            ChatMember chatMember = JSON.parseObject(member.toString(), ChatMember.class);
            String name = chatMember.getName();
            if (StringUtil.isBlank(name)) {
                return;
            }
            UserInfoUtil.putUser(name);
            String allTask = delayedTask.getAllTask(name);
            String portraitStr = Optional.ofNullable(redisUtil.getString(String.format(SelfUpdate.CUSTOM_SELF_PORTRAIT, name))).orElse(redisUtil.getString(SELF_PORTRAIT_KEY));
            SelfPortrait selfPortrait = JSON.parseObject(portraitStr, SelfPortrait.class);
            String planPrompt = promptFactory.getDayPlanPrompt(allTask, selfPortrait.toMarkDown());
            List<OpenAiApi.ChatCompletionMessage> messages = Lists.newArrayList(new OpenAiApi.ChatCompletionMessage(planPrompt, OpenAiApi.ChatCompletionMessage.Role.SYSTEM));
            springAiChat.generateMsgWithMsgListAndFunctions(messages, false, ChatSceneEnum.PLAN, 1.0f);
        });
    }

}
