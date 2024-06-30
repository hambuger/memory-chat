package com.github.hambuger.memory.chat.memory.chat;

import com.alibaba.fastjson.JSON;
import com.github.hambuger.memory.chat.memory.constants.Constants;
import com.github.hambuger.memory.chat.memory.util.CallFunctionRegistryFactory;

import jakarta.annotation.PostConstruct;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.langchain4j.agent.tool.ToolSpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import static com.github.hambuger.memory.chat.memory.constants.Constants.REPLY_MESSAGE_FUNCTION_NAME;
import static com.github.hambuger.memory.chat.memory.constants.Constants.REQUIRED;


/**
 * @author hanjiabao
 * @since 2024/6/23
 */
@Slf4j
@Component
public class SpringAiChat {

    @Value("${spring.ai.openai.api-key}")
    private String openaiApiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    private OpenAiApi openAiApi;

    @PostConstruct
    public void init() {
        ClientHttpRequestFactorySettings requestFactorySettings = new ClientHttpRequestFactorySettings(
                Duration.ofSeconds(10000) , Duration.ofSeconds(10000) , SslBundle.of(null));
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(requestFactorySettings);
        RestClient.Builder clientBuilder = RestClient.builder().requestFactory(requestFactory);
        openAiApi = new OpenAiApi(baseUrl, openaiApiKey, clientBuilder, WebClient.builder());
    }

    public OpenAiApi.ChatCompletion generateMsgWithMsgListAndFunctions(List<OpenAiApi.ChatCompletionMessage> messages) {
        OpenAiApi.ChatCompletionRequest chatRequest = new OpenAiApi.ChatCompletionRequest(messages, false);
        List<ToolSpecification> toolSpecifications = CallFunctionRegistryFactory.getAllFunctionCall();
        List<OpenAiApi.FunctionTool> tools = new ArrayList<>();
        for (ToolSpecification toolSpecification : toolSpecifications) {
            Map<String, Object> toolMap = new HashMap<>();
            toolMap.put("required", toolSpecification.parameters().required());
            toolMap.put("properties", toolSpecification.parameters().properties());
            toolMap.put("type", toolSpecification.parameters().type());
            OpenAiApi.FunctionTool chatTool = new OpenAiApi.FunctionTool(OpenAiApi.FunctionTool.Type.FUNCTION, new OpenAiApi.FunctionTool.Function(toolSpecification.description(),
                    toolSpecification.name(), JSON.toJSONString(toolMap)));
            tools.add(chatTool);
        }

        OpenAiChatOptions chatOptions =
                OpenAiChatOptions.builder().withModel(Constants.MODEL_NAME).withTools(tools).withToolChoice(REQUIRED).withTemperature(0.0f).build();
        chatRequest = ModelOptionsUtils.merge(chatOptions, chatRequest, OpenAiApi.ChatCompletionRequest.class);
        ResponseEntity<OpenAiApi.ChatCompletion> response = openAiApi.chatCompletionEntity(chatRequest);
        if (response == null || CollectionUtils.isEmpty(response.getBody().choices()) || response.getBody().choices().get(0).message().toolCalls().stream().anyMatch(tool -> tool.function().name().equals(REPLY_MESSAGE_FUNCTION_NAME))) {
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

    public OpenAiApi.ChatCompletion generateMsgWithMsgList(List<OpenAiApi.ChatCompletionMessage> messages) {
        OpenAiApi.ChatCompletionRequest chatRequest = new OpenAiApi.ChatCompletionRequest(messages, false);
        OpenAiChatOptions chatOptions =
                OpenAiChatOptions.builder().withModel(Constants.MODEL_NAME).withTemperature(0.0f).build();
        chatRequest = ModelOptionsUtils.merge(chatOptions, chatRequest, OpenAiApi.ChatCompletionRequest.class);
        ResponseEntity<OpenAiApi.ChatCompletion> response = openAiApi.chatCompletionEntity(chatRequest);
        return  response.getBody();
    }

}
