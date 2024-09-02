package com.github.hambuger.memory.chat.memory.memory.reflection;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.hambuger.memory.chat.memory.chat.SpringAiChat;
import com.github.hambuger.memory.chat.memory.other.prompt.PromptFactory;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;


/**
 * @author hamburger
 * @since 2024/6/13
 */
@Slf4j
@Component
public class MemoryReflection {

    @Resource
    private SpringAiChat springAiChat;

    @Resource
    private PromptFactory promptFactory;


    @Data
    public static class ReflectionResult {

        @JsonPropertyDescription("reflection内容列表")
        @JsonProperty(required = true)
        public List<Reflection> reflectionList = new ArrayList<>();

        @Data
        public static class Reflection {

            @JsonPropertyDescription("reflection内容")
            @JsonProperty(required = true)
            private String text = "";

            @JsonPropertyDescription("来源")
            @JsonProperty(required = true)
            private List<String> p_ids = new ArrayList<>();

        }

        public static String getJsonTemplate(){
            return """
                    {
                         "reflectionList": [
                             {
                                 "text": "",
                                 "p_ids": [
                                 ]
                             }
                         ]
                     }
                    """;
        }
    }


    public List<ReflectionResult.Reflection> extractReflectionFromMessages(String receiverName, List<String> msgList) {
        String result = springAiChat.generateJsonWithSingleMsgAndPrompt(promptFactory.getMsgReflectionPrompt(receiverName, StringUtils.join(msgList, "\n"), ReflectionResult.getJsonTemplate()), ReflectionResult.class);
        if (StringUtils.isBlank(result)) {
            return new ArrayList<>();
        }
        ReflectionResult reflectionResults = JSON.parseObject(result, ReflectionResult.class);
        return reflectionResults.getReflectionList();
    }

}
