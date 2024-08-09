package com.github.hambuger.memory.chat.memory.memory;

import com.alibaba.fastjson.JSON;
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

        public List<Reflection> reflectionList = new ArrayList<>();

        @Data
        public class Reflection {

            private String text = "";

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


    public List<ReflectionResult.Reflection> extractReflectionFromMessages(List<String> msgList) {
        String result = springAiChat.generateJsonWithSingleMsgAndPrompt(promptFactory.getMsgReflectionPrompt(StringUtils.join(msgList, "\n"), ReflectionResult.getJsonTemplate()));
        if (StringUtils.isBlank(result)) {
            return new ArrayList<>();
        }
        ReflectionResult reflectionResults = JSON.parseObject(result, ReflectionResult.class);
        return reflectionResults.getReflectionList();
    }

}
