package com.github.hambuger.memory.chat.memory.other.prompt;

import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.portrait.SelfUpdate;
import com.github.hambuger.memory.chat.memory.other.util.RedisUtil;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
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

    Map<ChatSceneEnum, String> chatStyleMap = new HashMap<>() {
        {
            put(ChatSceneEnum.NORMAL_USER, PromptTemplate.NORMAL_STYLE);
            put(ChatSceneEnum.NORMAL_GROUP, PromptTemplate.NORMAL_STYLE);
            put(ChatSceneEnum.SCHEDULE, PromptTemplate.NORMAL_STYLE);
            put(ChatSceneEnum.NEWS_SCHEDULE, PromptTemplate.NORMAL_STYLE);
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
        templateValueMap.put("styles", chatStyleMap.get(sceneEnum));
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

    public String getNewMsgCheckPrompt(String chatHistory, String friendName, String newMsg) {
        Map<String, String> templateValueMap = new HashMap<>();
        templateValueMap.put("chatHistory", chatHistory);
        templateValueMap.put("friendName", friendName);
        templateValueMap.put("newMsg", newMsg);
        templateValueMap.put("now", DateUtil.now());
        return formatPrompt(PromptTemplate.NEW_MSG_PROMPT, templateValueMap);
    }

    public String getEmotionPrompt() {
        return PromptTemplate.EMOTION_PROMPT;
    }

    public String getDayPlanPrompt(String task) {
        Map<String, String> templateValueMap = new HashMap<>();
        templateValueMap.put("now", DateUtil.format(new Date(), DatePattern.CHINESE_DATE_PATTERN) + "(" + DateUtil.dayOfWeekEnum(new Date()).toString() + ")");
        templateValueMap.put("task", StringUtils.isBlank(task) ? "无任务" : task);
        return formatPrompt(PromptTemplate.DAY_PLAN_PROMPT, templateValueMap);
    }


    public String getMemoryMergePrompt(String messageContent, String historyMemory) {
        Map<String, String> templateValueMap = new HashMap<>();
        templateValueMap.put("memory", messageContent);
        templateValueMap.put("existingMemories", historyMemory);
        return formatPrompt(PromptTemplate.MEMORY_MERGE_PROMPT, templateValueMap);
    }

    public String getLearnSkillPrompt(String memory) {
        return String.format(PromptTemplate.LEARN_SKILL_PROMPT, memory);
    }
}
