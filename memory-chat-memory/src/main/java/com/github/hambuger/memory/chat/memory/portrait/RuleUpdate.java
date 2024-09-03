package com.github.hambuger.memory.chat.memory.portrait;

import com.github.hambuger.memory.chat.memory.other.util.UserInfoUtil;
import com.google.common.collect.Lists;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.hambuger.memory.chat.memory.chat.SpringAiChat;
import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.other.functionCall.aop.FunctionCallRegistry;
import com.github.hambuger.memory.chat.memory.other.prompt.PromptFactory;
import com.github.hambuger.memory.chat.memory.other.util.RedisUtil;

import org.apache.poi.util.StringUtil;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import cn.hutool.core.collection.CollectionUtil;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;


/**
 * @author hamburger
 * @since 2024/8/27
 */
@Slf4j
@Component
public class RuleUpdate {

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private SpringAiChat springAiChat;

    @Resource
    private PromptFactory promptFactory;

    private static final String RULE_KEY = "selfRules";

    private static final String CUSTOM_RULE_KEY = "%s::selfRules";

    @Data
    public static class ChatRuleUpdate {

        @JsonPropertyDescription("Andrew的聊天是否合理")
        @JsonProperty(required = true)
        private Boolean chatContentReasonable;

        @JsonPropertyDescription("原因")
        @JsonProperty(required = true)
        private String reason;

        @JsonPropertyDescription("新加规则")
        @JsonProperty(required = true)
        private List<String> improvedRules;

    }

    public String getChatRules() {
        String ruleStr = Optional.ofNullable(redisUtil.getString(String.format(CUSTOM_RULE_KEY, UserInfoUtil.getUser()))).orElse(redisUtil.getString(RULE_KEY));
        if (StringUtil.isNotBlank(ruleStr)) {
            List<String> ruleList = JSON.parseArray(ruleStr, String.class);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < ruleList.size(); i++) {
                sb.append(i + 1).append(". ").append(ruleList.get(i));
                if (i < ruleList.size() - 1) {
                    sb.append("\n"); // 如果不是最后一个元素，添加逗号和空格
                }
            }
            return sb.toString();
        }
        return null;
    }

    public void checkAndMergeChatRules(String chatHistory) {
        String prompt = promptFactory.getCheckChatRulesPrompt(chatHistory);
        List<OpenAiApi.ChatCompletionMessage> messages = Lists.newArrayList(new OpenAiApi.ChatCompletionMessage(prompt, OpenAiApi.ChatCompletionMessage.Role.SYSTEM));
        springAiChat.generateMsgWithMsgListAndFunctions(messages, false, ChatSceneEnum.RULE_CHANGE);
    }

    @FunctionCallRegistry(functionDesc = "chat content judgment and processing", scene = {ChatSceneEnum.RULE_CHANGE})
    public Boolean chatContentJudgmentProcess(ChatRuleUpdate param) {
        if (param == null || param.getChatContentReasonable() || CollectionUtils.isEmpty(param.getImprovedRules())) {
            return true;
        }
        String ruleStr = Optional.ofNullable(redisUtil.getString(String.format(CUSTOM_RULE_KEY, UserInfoUtil.getUser()))).orElse(redisUtil.getString(RULE_KEY));
        List<String> finalRules = param.getImprovedRules();
        if (StringUtil.isNotBlank(ruleStr)) {
            List<String> ruleList = JSON.parseArray(ruleStr, String.class);
            if (!CollectionUtils.isEmpty(ruleList)) {
                ruleList.addAll(finalRules);
                if (ruleList.size() < 12) {
                    finalRules = ruleList;
                }else {
                    List<String> mergeRules = mergeRules(ruleList);
                    if (!CollectionUtils.isEmpty(mergeRules)) {
                        finalRules = mergeRules;
                    }
                }
            }
        }
        redisUtil.setString(String.format(CUSTOM_RULE_KEY, UserInfoUtil.getUser()), JSON.toJSONString(finalRules));
        return true;
    }

    @Data
    public static class MergeRules implements Serializable {

        @Serial
        private static final long serialVersionUID = -4177796120936939874L;

        @JsonPropertyDescription("merge Rule list")
        @JsonProperty(required = true)
        private List<String> mergeRuleList;

        @JsonPropertyDescription("理由")
        @JsonProperty(required = true)
        private String reason;

    }


    private List<String> mergeRules(List<String> ruleList) {
        String json = """
                {
                    "mergeRuleList": [],
                    "reason": ""
                }
                """;
        String ruleMergePrompt = promptFactory.getRuleMergePrompt(JSON.toJSONString(ruleList), json);
        String ruleStr = springAiChat.generateJsonWithSingleMsgAndPrompt(ruleMergePrompt, MergeRules.class);
        if (StringUtil.isNotBlank(ruleStr)) {
            JSONObject ruleObj = JSON.parseObject(ruleStr);
            JSONArray mergeRuleList = ruleObj.getJSONArray("mergeRuleList");
            if (CollectionUtil.isNotEmpty(mergeRuleList)) {
                return mergeRuleList.toJavaList(String.class);
            }
        }
        return null;
    }

}
