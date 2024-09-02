package com.github.hambuger.memory.chat.memory.chat;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.common.collect.Lists;

import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.other.functionCall.CallFunctionRegistryFactory;
import com.github.hambuger.memory.chat.memory.other.functionCall.aop.FunctionCallRegistry;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;

import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.formula.functions.T;
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
 * @author hamburger
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

    public OpenAiApi openAiApi;

    @Data
    public static class FinishParam {

        @JsonPropertyDescription("完成状态")
        @JsonProperty(required = true)
        boolean finishStatus;
    }

    @FunctionCallRegistry(functionDesc = "完成所有操作并获取到操作结果后，更新完成状态", scene = {ChatSceneEnum.PLAN, ChatSceneEnum.MEMORY_MERGE, ChatSceneEnum.TASK, ChatSceneEnum.LEARN_JUDGE, ChatSceneEnum.ROLE_CHANGE,ChatSceneEnum.UPDATE_FRIEND_PORTRAIT, ChatSceneEnum.UPDATE_SELF_PORTRAIT, ChatSceneEnum.RULE_CHANGE})
    public Boolean updateFinishFlag(FinishParam success) {
        return true;
    }

    @PostConstruct
    public void init() {
        ClientHttpRequestFactorySettings requestFactorySettings = new ClientHttpRequestFactorySettings(
                Duration.ofSeconds(10000) , Duration.ofSeconds(10000) , SslBundle.of(null));
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(requestFactorySettings);
        RestClient.Builder clientBuilder = RestClient.builder().requestFactory(requestFactory);
        openAiApi = new OpenAiApi(baseUrl, openaiApiKey, clientBuilder, WebClient.builder());
    }

    public OpenAiApi.ChatCompletion generateMsgWithMsgListAndFunctions(List<OpenAiApi.ChatCompletionMessage> messages, boolean groupFlag, ChatSceneEnum scene) {
        return generateMsgWithMsgListAndFunctions(messages, groupFlag, scene, this.temperature);
    }

    public OpenAiApi.ChatCompletion generateMsgWithMsgListAndFunctions(List<OpenAiApi.ChatCompletionMessage> messages, boolean groupFlag, ChatSceneEnum scene, Float temperature) {
        OpenAiApi.ChatCompletionRequest chatRequest = new OpenAiApi.ChatCompletionRequest(messages, false);
        List<OpenAiApi.FunctionTool> tools = CallFunctionRegistryFactory.getAllFunctionCall(groupFlag, scene);

        OpenAiChatOptions chatOptions =
                OpenAiChatOptions.builder().withModel(modelName).withTools(tools).withToolChoice(REQUIRED).withTemperature(Optional.ofNullable(temperature).orElse(this.temperature)).build();
        switchImageModel(messages, chatOptions);
        chatRequest = ModelOptionsUtils.merge(chatOptions, chatRequest, OpenAiApi.ChatCompletionRequest.class);
        ResponseEntity<OpenAiApi.ChatCompletion> response = openAiApi.chatCompletionEntity(chatRequest);
        if (response == null || CollectionUtils.isEmpty(response.getBody().choices())
                || CollectionUtils.isEmpty(response.getBody().choices().get(0).message().toolCalls())
                || (scene.getEndFunctionName() != null  && response.getBody().choices().get(0).message().toolCalls().size() == 1
                && response.getBody().choices().get(0).message().toolCalls().get(0).function().name().equals(scene.getEndFunctionName()))) {
            return response.getBody();
        }
        List<OpenAiApi.ChatCompletionMessage> executionResultMessages = new ArrayList<>();
        for (OpenAiApi.ChatCompletionMessage.ToolCall toolExecution : response.getBody().choices().get(0).message().toolCalls()) {
            String funResult = CallFunctionRegistryFactory.executeFunctionResult(toolExecution.function().name(), toolExecution.function().arguments());
            OpenAiApi.ChatCompletionMessage functionMsg = new OpenAiApi.ChatCompletionMessage(funResult, OpenAiApi.ChatCompletionMessage.Role.TOOL, toolExecution.function().name(),
                    toolExecution.id(), null, null);
            executionResultMessages.add(functionMsg);
        }
        messages.add(response.getBody().choices().get(0).message());
        messages.addAll(executionResultMessages);
        return generateMsgWithMsgListAndFunctions(messages, groupFlag, scene, temperature);
    }

    public OpenAiApi.ChatCompletion generateMsgWithMsgList(List<OpenAiApi.ChatCompletionMessage> messages, boolean jsonFormat) {
        OpenAiApi.ChatCompletionRequest chatRequest = new OpenAiApi.ChatCompletionRequest(messages, false);
        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder().withModel(modelName).withTemperature(temperature).build();
        if (jsonFormat) {
            chatOptions.setResponseFormat(new OpenAiApi.ChatCompletionRequest.ResponseFormat(OpenAiApi.ChatCompletionRequest.ResponseFormat.Type.JSON_OBJECT));
        }
        switchImageModel(messages, chatOptions);
        chatRequest = ModelOptionsUtils.merge(chatOptions, chatRequest, OpenAiApi.ChatCompletionRequest.class);
        ResponseEntity<OpenAiApi.ChatCompletion> response = openAiApi.chatCompletionEntity(chatRequest);
        return  response.getBody();
    }

    private void switchImageModel(List<OpenAiApi.ChatCompletionMessage> messages, OpenAiChatOptions chatOptions) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }
        boolean imageFlag = messages.stream().anyMatch(msg -> {
            if (!msg.role().equals(OpenAiApi.ChatCompletionMessage.Role.USER)) {
                return false;
            }
            if (msg.rawContent() instanceof List) {
                return ((List<?>) msg.rawContent()).stream().anyMatch(obj -> {
                    OpenAiApi.ChatCompletionMessage.MediaContent mediaContent = (OpenAiApi.ChatCompletionMessage.MediaContent) obj;
                    return StringUtils.equals(mediaContent.type(), "image_url");
                });
            } else {
                return false;
            }
        });
        if (imageFlag) {
            chatOptions.setModel(OpenAiApi.ChatModel.GPT_4_O.getName());
        }
    }

    public OpenAiApi.ChatCompletion generateMsgWithMsgList(List<OpenAiApi.ChatCompletionMessage> messages, Class<T> paramClass, String paramName) {
        try {
            OpenAiApi.ChatCompletionRequest chatRequest = new OpenAiApi.ChatCompletionRequest(messages, false);
            OpenAiChatOptions chatOptions = OpenAiChatOptions.builder().withModel(modelName).withTemperature(temperature).build();
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
            JsonSchemaGenerator jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper);
            JsonNode jsonSchema = jsonSchemaGenerator.generateJsonSchema(paramClass);
            chatOptions.setResponseFormat(new OpenAiApi.ChatCompletionRequest.ResponseFormat(OpenAiApi.ChatCompletionRequest.ResponseFormat.Type.JSON_SCHEMA,
                    new OpenAiApi.ChatCompletionRequest.ResponseFormat.JsonSchema(paramName, objectMapper.writeValueAsString(jsonSchema))));
            chatRequest = ModelOptionsUtils.merge(chatOptions, chatRequest, OpenAiApi.ChatCompletionRequest.class);
            ResponseEntity<OpenAiApi.ChatCompletion> response = openAiApi.chatCompletionEntity(chatRequest);
            return response.getBody();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public String generateJsonWithSingleMsgAndPrompt(String prompt, Class paramClass) {
        List<OpenAiApi.ChatCompletionMessage> messages = Lists.newArrayList(new OpenAiApi.ChatCompletionMessage(prompt, OpenAiApi.ChatCompletionMessage.Role.SYSTEM));
        OpenAiApi.ChatCompletion chatCompletion = generateMsgWithMsgList(messages, paramClass, paramClass.getSimpleName());
        return Optional.ofNullable(chatCompletion).map(OpenAiApi.ChatCompletion::choices).map(list -> list.get(0)).map(OpenAiApi.ChatCompletion.Choice::message).map(OpenAiApi.ChatCompletionMessage::content).orElse(null);
    }

    public String generateJsonWithSingleMsgAndPrompt(String prompt, String userMsg, Class paramClass) {
        List<OpenAiApi.ChatCompletionMessage> messages = Lists.newArrayList(new OpenAiApi.ChatCompletionMessage(prompt, OpenAiApi.ChatCompletionMessage.Role.SYSTEM),
                new OpenAiApi.ChatCompletionMessage(userMsg, OpenAiApi.ChatCompletionMessage.Role.USER));
        OpenAiApi.ChatCompletion chatCompletion = generateMsgWithMsgList(messages, paramClass, paramClass.getSimpleName());
        return Optional.ofNullable(chatCompletion).map(OpenAiApi.ChatCompletion::choices).map(list -> list.get(0)).map(OpenAiApi.ChatCompletion.Choice::message).map(OpenAiApi.ChatCompletionMessage::content).orElse(null);
    }

}
