package com.github.hambuger.memory.chat.memory.chat;

import com.google.common.collect.Lists;

import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.other.constants.MemoryChatConstants;
import com.github.hambuger.memory.chat.memory.other.functionCall.CallFunctionRegistryFactory;

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
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import static com.github.hambuger.memory.chat.memory.other.constants.MemoryChatConstants.REQUIRED;


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

    @Value("${spring.ai.openai.temperature}")
    private Float temperature;

    @Value("${spring.ai.openai.chat.options.model}")
    private String modelName;

    private OpenAiApi openAiApi;

    @PostConstruct
    public void init() {
        ClientHttpRequestFactorySettings requestFactorySettings = new ClientHttpRequestFactorySettings(
                Duration.ofSeconds(10000) , Duration.ofSeconds(10000) , SslBundle.of(null));
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(requestFactorySettings);
        RestClient.Builder clientBuilder = RestClient.builder().requestFactory(requestFactory);
        openAiApi = new OpenAiApi(baseUrl, openaiApiKey, clientBuilder, WebClient.builder());
    }

    public OpenAiApi.ChatCompletion generateMsgWithMsgListAndFunctions(List<OpenAiApi.ChatCompletionMessage> messages, boolean groupFlag, ChatSceneEnum scene) {
        OpenAiApi.ChatCompletionRequest chatRequest = new OpenAiApi.ChatCompletionRequest(messages, false);
        List<OpenAiApi.FunctionTool> tools = CallFunctionRegistryFactory.getAllFunctionCall(groupFlag, scene);

        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder().withModel(modelName).withTools(tools).withToolChoice(REQUIRED).withTemperature(temperature).build();
        chatRequest = ModelOptionsUtils.merge(chatOptions, chatRequest, OpenAiApi.ChatCompletionRequest.class);
        ResponseEntity<OpenAiApi.ChatCompletion> response = openAiApi.chatCompletionEntity(chatRequest);
        if (response == null || CollectionUtils.isEmpty(response.getBody().choices())
                || CollectionUtils.isEmpty(response.getBody().choices().get(0).message().toolCalls())
                || (scene.getEndFunctionName() != null
                && response.getBody().choices().get(0).message().toolCalls().stream().anyMatch(tool -> tool.function().name().equals(scene.getEndFunctionName())))) {
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
        return generateMsgWithMsgListAndFunctions(messages, groupFlag, scene);
    }

    public OpenAiApi.ChatCompletion generateMsgWithMsgList(List<OpenAiApi.ChatCompletionMessage> messages, boolean jsonFormat) {
        OpenAiApi.ChatCompletionRequest chatRequest = new OpenAiApi.ChatCompletionRequest(messages, false);
        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder().withModel(modelName).withTemperature(temperature).build();
        if (jsonFormat) {
            chatOptions.setResponseFormat(new OpenAiApi.ChatCompletionRequest.ResponseFormat(MemoryChatConstants.JSON_OBJECT));
        }
        chatRequest = ModelOptionsUtils.merge(chatOptions, chatRequest, OpenAiApi.ChatCompletionRequest.class);
        ResponseEntity<OpenAiApi.ChatCompletion> response = openAiApi.chatCompletionEntity(chatRequest);
        return  response.getBody();
    }

    public String generateJsonWithSingleMsgAndPrompt(String prompt) {
        List<OpenAiApi.ChatCompletionMessage> messages = Lists.newArrayList(new OpenAiApi.ChatCompletionMessage(prompt,
                OpenAiApi.ChatCompletionMessage.Role.USER));
        OpenAiApi.ChatCompletion chatCompletion = generateMsgWithMsgList(messages, true);
        return Optional.ofNullable(chatCompletion).map(OpenAiApi.ChatCompletion::choices).map(list -> list.get(0)).map(OpenAiApi.ChatCompletion.Choice::message).map(OpenAiApi.ChatCompletionMessage::content).orElse(null);
    }

}
