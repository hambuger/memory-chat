package com.github.hambuger.memory.chat.memory.chat;

import com.alibaba.fastjson.JSON;
import com.github.hambuger.memory.chat.memory.constants.Constants;
import com.github.hambuger.memory.chat.memory.util.CallFunctionRegistryFactory;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import dev.langchain4j.agent.tool.ToolSpecification;
import lombok.extern.slf4j.Slf4j;


/**
 * @author hanjiabao
 * @since 2024/6/23
 */
@Slf4j
@Component
public class SpringAiChat {

    @Autowired
    private OpenAiApi openAiApi;


    public OpenAiApi.ChatCompletion generateMsgWithMsgListAndFunctions(List<OpenAiApi.ChatCompletionMessage> messages) {
        OpenAiApi.ChatCompletionRequest chatRequest = new OpenAiApi.ChatCompletionRequest(messages, false);
        ;
        List<ToolSpecification> toolSpecifications = CallFunctionRegistryFactory.getAllFunctionCall();
        List<OpenAiApi.FunctionTool> tools = new ArrayList<>();
        for (ToolSpecification toolSpecification : toolSpecifications) {
            OpenAiApi.FunctionTool chatTool = new OpenAiApi.FunctionTool(OpenAiApi.FunctionTool.Type.FUNCTION, JSON.parseObject(JSON.toJSONString(toolSpecification),
                    OpenAiApi.FunctionTool.Function.class));
            tools.add(chatTool);
        }

        OpenAiChatOptions chatOptions =
                OpenAiChatOptions.builder().withModel(Constants.MODEL_NAME).withTools(tools).withToolChoice("required").withMaxTokens(Constants.MAX_MSG_TOKEN).withTemperature(0.0f).build();
        ModelOptionsUtils.merge(chatOptions, chatRequest, OpenAiApi.ChatCompletionRequest.class);
        ResponseEntity<OpenAiApi.ChatCompletion> response = openAiApi.chatCompletionEntity(chatRequest);
        if (response == null || CollectionUtils.isEmpty(response.getBody().choices()) || response.getBody().choices().get(0).message().toolCalls().stream().anyMatch(tool -> tool.function().name().equals("sendWechatMessage"))) {
            return response.getBody();
        }
        List<OpenAiApi.ChatCompletionMessage> executionResultMessages = new ArrayList<>();
        for (OpenAiApi.ChatCompletionMessage.ToolCall toolExecution : response.getBody().choices().get(0).message().toolCalls()) {
            String funResult = CallFunctionRegistryFactory.executeFunctionResult(toolExecution.function().name(), toolExecution.function().arguments());
            OpenAiApi.ChatCompletionMessage functionMsg = new OpenAiApi.ChatCompletionMessage(funResult, OpenAiApi.ChatCompletionMessage.Role.TOOL, toolExecution.function().name(),
                    toolExecution.id(), null);
            executionResultMessages.add(functionMsg);
        }
        messages.add(response.getBody().choices().get(0).message());
        messages.addAll(executionResultMessages);
        return generateMsgWithMsgListAndFunctions(messages);
    }

}
