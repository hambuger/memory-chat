package com.github.hambuger.memory.chat.memory.memory.create;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.hambuger.memory.chat.memory.chat.SpringAiChat;
import com.github.hambuger.memory.chat.memory.other.prompt.PromptFactory;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.Serial;
import java.io.Serializable;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;


/**
 * @author hamburger
 * @since 2024/6/13
 */
@Slf4j
@Component
public class MemoryImportantScore {

    @Resource
    private SpringAiChat springAiChat;

    @Resource
    private PromptFactory promptFactory;

    public static class ScoreResult implements Serializable {

        @Serial
        private static final long serialVersionUID = -1242490779818522911L;

        @JsonPropertyDescription("importance score，0.0-1.0")
        @JsonProperty(required = true)
        private double score;
    }

    public Double generateImportantScore(String message) {
        String score = JSONObject.parseObject(springAiChat.generateJsonWithSingleMsgAndPrompt(promptFactory.getMsgScorePrompt(message), ScoreResult.class)).getString("score");
        if (StringUtils.isNotBlank(score)) {
            return Double.valueOf(score);
        }
        return 0.0;
    }

}
