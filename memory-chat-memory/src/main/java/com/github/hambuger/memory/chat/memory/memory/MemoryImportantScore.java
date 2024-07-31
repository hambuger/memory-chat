package com.github.hambuger.memory.chat.memory.memory;

import com.alibaba.fastjson.JSONObject;
import com.github.hambuger.memory.chat.memory.chat.SpringAiChat;
import com.github.hambuger.memory.chat.memory.prompt.ChatPrompt;
import com.github.hambuger.memory.chat.memory.prompt.PromptFactory;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

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
    private ChatPrompt chatPrompt;

    @Resource
    private PromptFactory promptFactory;


    public Double generateImportantScore(String message) {
        String score = JSONObject.parseObject(springAiChat.generateJsonWithSingleMsgAndPrompt(promptFactory.getMsgScorePrompt(message))).getString("score");
        if (StringUtils.isNotBlank(score)) {
            return Double.valueOf(score);
        }
        return 0.0;
    }

}
