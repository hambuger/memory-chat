package com.github.hambuger.memory.chat.memory.other.prompt;

import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.other.util.UserInfoUtil;
import com.github.hambuger.memory.chat.memory.portrait.RuleUpdate;
import com.github.hambuger.memory.chat.memory.portrait.SelfUpdate;
import com.github.hambuger.memory.chat.memory.other.util.RedisUtil;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
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
 * @author hamburger
 * @since 2024/7/24
 */
@Slf4j
@Component
public class PromptFactory {

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private SelfUpdate selfUpdate;

    @Resource
    private RuleUpdate ruleUpdate;

    @Value("${env.language:zh}")
    private String language;

    public boolean isZh(){
        return StringUtils.equals(language, "zh");
    }

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
            String template = isZh() ? PromptTemplate.NORMAL_STYLE : PromptTemplate.NORMAL_STYLE_EN;
            put(ChatSceneEnum.NORMAL_USER, template);
            put(ChatSceneEnum.NORMAL_GROUP, template);
            put(ChatSceneEnum.SCHEDULE, template);
            put(ChatSceneEnum.NEWS_SCHEDULE, template);
        }
    };


    public String getChatPrompt(String messageFromName, String chatHistory, String news, boolean groupFlag, ChatSceneEnum sceneEnum) {
        Map<String, String> templateValueMap = new HashMap<>();
        templateValueMap.put("selfPortrait", Optional.ofNullable(selfUpdate.getSelfPortrait()).orElse(""));
        templateValueMap.put("friendName", messageFromName);
        templateValueMap.put("talkingDesc", groupFlag ? "one of the WeChat groups you joined" : "is your WeChat Friend");
        templateValueMap.put("talkingRole", groupFlag ? "this group" : "him/she");
        templateValueMap.put("talkingInfo", Optional.ofNullable(groupFlag ? redisUtil.getGroupPortrait(messageFromName) : redisUtil.getFriendPortrait(messageFromName)).orElse(String.format("- Name: %s", messageFromName)));
        templateValueMap.put("memory", chatHistory);
        if (sceneEnum == ChatSceneEnum.NORMAL_GROUP || sceneEnum == ChatSceneEnum.NORMAL_USER) {
            templateValueMap.put("rules", Optional.ofNullable(ruleUpdate.getChatRules()).orElse(chatRuleMap.get(sceneEnum)));
        }else {
            templateValueMap.put("rules", chatRuleMap.get(sceneEnum));
        }
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


    public String getCheckChatRulesPrompt(String chatHistory) {
        return String.format(isZh() ? PromptTemplate.CHECK_RULE_PROMPT : PromptTemplate.CHECK_RULE_PROMPT_EN, chatHistory);
    }


    public String getGenerateCustomChatModelPrompt() {
        return isZh() ? PromptTemplate.GENERATE_CUSTOM_MODEL_PROMPT : PromptTemplate.GENERATE_CUSTOM_MODEL_PROMPT_EN;
    }

    public String getLearnCodeSkillPrompt(String memory) {
        return String.format(PromptTemplate.CODE_LEARN_PROMPT, memory);
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
        return formatPrompt(isZh() ? PromptTemplate.NEW_MSG_PROMPT : PromptTemplate.NEW_MSG_PROMPT_EN, templateValueMap);
    }

    public String getEmotionPrompt() {
        return isZh() ? PromptTemplate.EMOTION_PROMPT : PromptTemplate.EMOTION_PROMPT_EN;
    }

    public String getDayPlanPrompt(String task, String selfPortrait) {
        Map<String, String> templateValueMap = new HashMap<>();
        templateValueMap.put("now", DateUtil.format(new Date(), DatePattern.CHINESE_DATE_PATTERN) + "(" + DateUtil.dayOfWeekEnum(new Date()).toString() + ")");
        templateValueMap.put("task", StringUtils.isBlank(task) ? "None" : task);
        templateValueMap.put("selfPortrait", selfPortrait);
        return formatPrompt(isZh() ? PromptTemplate.DAY_PLAN_PROMPT : PromptTemplate.DAY_PLAN_PROMPT_EN, templateValueMap);
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


    public String getRoleContentCheckPrompt(String messageContent, String json) {
        return String.format(PromptTemplate.ROLE_CHECK_PROMPT, messageContent, json);
    }

    public String getRuleMergePrompt(String messageContent, String json) {
        return String.format(isZh() ? PromptTemplate.RULE_MERGE_PROMPT : PromptTemplate.RULE_MERGE_PROMPT_EN, messageContent, json);
    }


    public String getRoleDetailPrompt(String content) {
        return String.format(PromptTemplate.ROLE_PROMPT, content);
    }

    public String getFriendPortraitUpdatePrompt(String name, String beforePortrait, String afterPortrait) {
        return String.format(PromptTemplate.FRIEND_PORTRAIT_UPDATE_PROMPT, name, beforePortrait, afterPortrait);
    }

    public String getSelfPortraitUpdatePrompt(String beforePortrait, String afterPortrait) {
        return String.format(PromptTemplate.SELF_PORTRAIT_UPDATE_PROMPT, beforePortrait, afterPortrait);
    }

    public String getMidFlowPrompt(String history) {
        Map<String, String> templateValueMap = new HashMap<>();
        templateValueMap.put("selfPortrait", Optional.ofNullable(selfUpdate.getSelfPortrait()).orElse(""));
        templateValueMap.put("friendPortrait", redisUtil.getFriendPortrait(UserInfoUtil.getUser()));
        templateValueMap.put("friend", UserInfoUtil.getUser());
        templateValueMap.put("history", history);
        templateValueMap.put("now", DateUtil.now());
        return formatPrompt(isZh() ? PromptTemplate.MID_FLOW_PROMPT : PromptTemplate.MID_FLOW_PROMPT_EN, templateValueMap);
    }

    public String getEmojiExtraPrompt() {
        return isZh() ? PromptTemplate.EMOJI_EXTRA_PROMPT : PromptTemplate.EMOJI_EXTRA_PROMPT_EN;
    }

    public String getAudioPrompt() {
        return isZh() ? PromptTemplate.AUDIO_PROMPT : PromptTemplate.AUDIO_PROMPT_EN;
    }

    public String getEmptySettingPrompt() {
        return isZh() ? "你提供的内容为空，重新输入！" : "The content you provided is empty, please re-enter!";
    }

    public String getResultSettingPrompt() {
        return isZh() ? "修改设定如下:\n" : "Modify the settings as follows:\n";
    }

    public String getErrorSettingPrompt() {
        return isZh() ? "你提供的内容和设定无关！" : "The content you provided has nothing to do with the setting!";
    }

    public String getTimePrompt(String messageCreatorName, String time) {
        return String.format(isZh() ? "距离上一次发送消息给%s已经过去了%s,中间对方没有任何回复" : "Since the last message was sent to %s，%s have passed , and the other party has not responded in the meantime", messageCreatorName, time);
    }

    public Object getTimePromptV2(String messageCreatorName, String time, String mindFlowStr) {
        return String.format(isZh() ? "距离上一次发送消息给%s已经过去了%s,中间对方没有任何回复。【你的内心活动：%s】" : "Since the last message was sent to %s，%s have passed , and the other party has not responded in the meantime.[Your inner thoughts: %s]", messageCreatorName, time, mindFlowStr);
    }

    public String getWeatherChangePrompt(String city, String beforeWeather, String weatherStr) {
        return String.format(isZh() ? "%s的天气由%s变成了%s" : "The weather of %s changed from %s to %s", city, beforeWeather, weatherStr);
    }

    public String getFileSendPrompt() {
        return isZh() ? "发送过来一个文件，文件地址：" : "Send a file, file url:";
    }

    public String getFileSendPromptV2() {
        return isZh() ? "文件的内容大致总结如下：\n" : "The contents of the document are roughly summarized as follows:\n";
    }

    public String getVideoInfoPrompt(String creatorName, String videoInfo) {
        return String.format(isZh() ? "%s发送了一个视频。这个视频的信息如下：%s" : "%s sent a video. The information of this video is as follows: %s", creatorName, videoInfo);
    }

    public String getThoughtPrompt(String mind) {
        return String.format(isZh() ? "[内心活动：%s]" : "[Inner thoughts: %s]", mind);
    }

    public String getDelayTaskPrompt(String time, String taskMessage, String now) {
        return String.format(isZh() ? """
你是Andrew,一个30岁的中国年轻人。
你在%s之前给自己写了一个便签，以便提醒自己要做的事情。
以下是便签的内容:
%s

按照便签的指引完成要做的事。
现在时间点是：%s
 """ : """
You are Andrew, a 30-year-old Chinese young man.
You wrote a note to yourself %s ago to remind yourself what to do.
The following is the content of the note:
%s

Follow the instructions of the note to complete what you need to do.
The current time is: %s""", time, taskMessage, now);
    }
}
