package com.github.hambuger.memory.chat.memory.memory.create;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.github.hambuger.memory.chat.memory.chat.SpringAiChat;
import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.other.functionCall.aop.FunctionCallRegistry;
import com.github.hambuger.memory.chat.memory.memory.model.MemoryDimensionInfo;
import com.github.hambuger.memory.chat.memory.other.prompt.PromptFactory;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;


/**
 * @author hanjiabao
 * @since 2024/8/6
 */
@Slf4j
@Component
public class MemoryDimensionGenerate {

    @Resource
    private SpringAiChat springAiChat;

    @Resource
    private PromptFactory promptFactory;

    @FunctionCallRegistry(functionDesc = "新增一个新的记忆", scene = {ChatSceneEnum.MEMORY_DIMENSION})
    public boolean addNewMemory(MemoryDimensionInfo memoryDimensionInfo) {
        return true;
    }

    public MemoryDimensionInfo generateDimension(List<OpenAiApi.ChatCompletionMessage> messages) {
        OpenAiApi.ChatCompletion chatCompletion = springAiChat.generateMsgWithMsgListAndFunctions(messages, false, ChatSceneEnum.MEMORY_DIMENSION);
        String dimensionInfoArgs =
                Optional.ofNullable(chatCompletion).map(OpenAiApi.ChatCompletion::choices).map(list -> list.get(0)).map(OpenAiApi.ChatCompletion.Choice::message).map(OpenAiApi.ChatCompletionMessage::toolCalls).map(list -> list.get(0)).map(OpenAiApi.ChatCompletionMessage.ToolCall::function).map(OpenAiApi.ChatCompletionMessage.ChatCompletionFunction::arguments).orElse(null);
        if (StringUtils.isNotBlank(dimensionInfoArgs)) {
            return JSON.parseObject(dimensionInfoArgs, MemoryDimensionInfo.class, Feature.IgnoreNotMatch);
        }
        return null;
    }

}
