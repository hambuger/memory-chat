package com.github.hambuger.memory.chat.memory.prompt;

import com.github.hambuger.memory.chat.memory.chat.dto.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.plan.SelfUpdate;
import com.github.hambuger.memory.chat.memory.util.RedisUtil;

import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;


/**
 * @author hanjiabao
 * @since 2024/7/24
 */
@Slf4j
@Component
public class PromptFactory {

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private SelfUpdate selfUpdate;

    Map<ChatSceneEnum, String> chatRuleMap = new HashMap<>() {
        {
            put(ChatSceneEnum.SCHEDULE, PromptTemplate.SCHEDULE_CHAT_RULES);
            put(ChatSceneEnum.NORMAL_USER, PromptTemplate.NORMAL_CHAT_RULES);
            put(ChatSceneEnum.NORMAL_GROUP, PromptTemplate.NORMAL_CHAT_RULES);
            put(ChatSceneEnum.NEWS_SCHEDULE, PromptTemplate.NEWS_SCHEDULE_RULES);
        }
    };

    Map<ChatSceneEnum, String> chatStepMap = new HashMap<>() {
        {
            put(ChatSceneEnum.SCHEDULE, PromptTemplate.NORMAL_STEPS);
            put(ChatSceneEnum.NORMAL_USER, PromptTemplate.NORMAL_STEPS);
            put(ChatSceneEnum.NORMAL_GROUP, PromptTemplate.NORMAL_STEPS);
            put(ChatSceneEnum.NEWS_SCHEDULE, PromptTemplate.NEWS_SCHEDULE_STEPS);
        }
    };


    public String getChatPrompt(String messageFromName, String chatHistory, String news, boolean groupFlag, ChatSceneEnum sceneEnum) {
        Map<String, String> templateValueMap = new HashMap<>();
        templateValueMap.put("selfPlanAndStatus", Optional.ofNullable(selfUpdate.getSelfPortrait()).orElse(""));
        templateValueMap.put("friendName", messageFromName);
        templateValueMap.put("talkingDesc", groupFlag ? "one of the WeChat groups you joined" : "is your WeChat Friend");
        templateValueMap.put("talkingRole", groupFlag ? "this group" : "him/she");
        templateValueMap.put("talkingInfo", Optional.ofNullable(groupFlag ? redisUtil.getGroupPortrait(messageFromName) : redisUtil.getFriendPortrait(messageFromName)).orElse(String.format("- Name: %s", messageFromName)));
        templateValueMap.put("memory", chatHistory);
        templateValueMap.put("rules", chatRuleMap.get(sceneEnum));
        templateValueMap.put("steps", chatStepMap.get(sceneEnum));
        if (sceneEnum == ChatSceneEnum.NEWS_SCHEDULE) {
            templateValueMap.put("hotNews", String.format(PromptTemplate.HOT_NEWS, news));
        }
        templateValueMap.put("now", DateUtil.format(new Date(), DatePattern.NORM_DATETIME_FORMAT) + "(" + DateUtil.dayOfWeekEnum(new Date()).toString() + ")");
        return formatPrompt(PromptTemplate.CHAT_PROMPT, templateValueMap);
    }


    private String formatPrompt(String template, Map<String, String> valueMap) {
        if (MapUtils.isEmpty(valueMap)) {
            return template;
        }
        for (Map.Entry<String, String> entry : valueMap.entrySet()) {
            String replaceStr = "${" + entry.getKey() + "}";
            if (template.contains(replaceStr)) {
                template = template.replace("${" + entry.getKey() + "}", Optional.ofNullable(entry.getValue()).orElse(""));
            }
        }
        return template.replaceAll("\\$\\{[^}]+}", "");
    }

    public String getMsgReflectionPrompt(String... param) {
        return String.format(PromptTemplate.REFLECTION_PROMPT, param);
    }


    public String getMsgScorePrompt(String... param) {
        return String.format(PromptTemplate.SCORE_PROMPT, param);
    }

}
