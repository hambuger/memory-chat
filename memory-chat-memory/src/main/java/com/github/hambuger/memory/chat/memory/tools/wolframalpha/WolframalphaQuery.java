package com.github.hambuger.memory.chat.memory.tools.wolframalpha;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.other.functionCall.aop.FunctionCallRegistry;
import com.github.hambuger.memory.chat.memory.other.util.MyHttpUtils;
import com.mashape.unirest.http.exceptions.UnirestException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


/**
 * @author hanjiabao
 * @since 2024/8/9
 */
@Slf4j
@Component
public class WolframalphaQuery {

    @Value("${wolframalpha.key}")
    private String wolframalphaKey;

    private static final String WOLFRAMALPHA_URL = "https://www.wolframalpha.com/api/v1/llm-api?appid=%s&input=%s?&output=json";


    @Data
    public static class WolframalphaParam {

        @JsonPropertyDescription("查询描述,只能使用英文")
        @JsonProperty(required = true)
        private String queryUseEnglishLanguage;

    }


    @FunctionCallRegistry(functionDesc = "通过wolframalpha查询结果，适合数学计算或者偏数学相关的问题", scene = {ChatSceneEnum.NORMAL_USER, ChatSceneEnum.NORMAL_GROUP, ChatSceneEnum.SCHEDULE, ChatSceneEnum.NEWS_SCHEDULE,
            ChatSceneEnum.TASK, ChatSceneEnum.PLAN})
    public String searchByWolframalpha(WolframalphaParam query) throws Exception {
        String wolframalphaUrl = String.format(WOLFRAMALPHA_URL, wolframalphaKey, URLEncoder.encode(query.getQueryUseEnglishLanguage(), StandardCharsets.UTF_8));
        return MyHttpUtils.get(wolframalphaUrl, null, null);
    }
}
