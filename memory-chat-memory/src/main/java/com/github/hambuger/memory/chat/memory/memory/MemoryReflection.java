package com.github.hambuger.memory.chat.memory.memory;

import com.alibaba.fastjson.JSON;
import com.github.hambuger.memory.chat.memory.chat.SpringAiChat;

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

    @Data
    public static class ReflectionResult {

        public List<Reflection> reflectionList = new ArrayList<>();

        @Data
        public class Reflection {

            private String text = "";

            private List<String> p_ids = new ArrayList<>();

        }
    }


    public List<ReflectionResult.Reflection> extractReflectionFromMessages(List<String> msgList) {
        String prompt = """
                From following historical records, extract information similar to human long-term memory.
                %s
                Make sure your answer can be parsed correctly into json data similar to the following.
                %s
                The text represents the summarized and refined content. It should be more concise and shorter than the original text.
                p_ids represents all the information sources that the abstract relies on, obtained from parentheses at the beginning of each conversation.
                """;
        String result = springAiChat.generateJsonWithSingleMsgAndPrompt(String.format(prompt, StringUtils.join(msgList, ";;"), JSON.toJSONString(new ReflectionResult())));
        if (StringUtils.isBlank(result)) {
            return new ArrayList<>();
        }
        ReflectionResult reflectionResults = JSON.parseObject(result, ReflectionResult.class);
        return reflectionResults.getReflectionList();
    }

}
