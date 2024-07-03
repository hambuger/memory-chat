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
        String prompt =
                "The ones between ```` below are past chat records.\n" + "\n" + "These conversations cover a variety of topics, from the trivialities of everyday life to discussions on a " +
                        "variety of topics.\n" + "\n" + "These memories may include your host's interests, opinions expressed in past conversations, important life events, and more.\n" + "\n" +
                        "Information similar to human long-term memory is extracted from these historical chat records.\n" + "\n" + "You only need json data like this in your answer, make sure your" +
                        " answer " + "can be parsed into json data correctly.\n" + JSON.toJSONString(new ReflectionResult()) + "\n" + "Among them, text indicates the content of " +
                        "the summary and " + "refinement. p_ids represents all the information sources that the abstract relies on, obtained from parentheses at the beginning of each conversation" +
                        ".\n" + "\n" + "````\n" + StringUtils.join(msgList, ";;") + "\n````";
        String result = springAiChat.generateJsonWithSingleMsgAndPrompt(prompt);
        if (StringUtils.isBlank(result)) {
            return new ArrayList<>();
        }
        ReflectionResult reflectionResults = JSON.parseObject(result, ReflectionResult.class);
        return reflectionResults.getReflectionList();
    }

}
