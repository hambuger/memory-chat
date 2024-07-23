package com.github.hambuger.memory.chat.memory.other.langchain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;

import java.util.List;

import dev.langchain4j.agent.tool.ToolParameters;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.output.Response;


/**
 * @author hamburger
 * @since 2024/6/13
 */
public class LangChainFunctionCall {

    public static AiMessage generateWithFunctionCall(Class argPojoClass, String methodName, String methodDesc, List<ChatMessage> messages) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        JsonSchemaGenerator jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper);

        JsonNode jsonSchema = jsonSchemaGenerator.generateJsonSchema(argPojoClass);

        ToolSpecification toolSpecification = null;
        try {
            ToolParameters toolParameters = objectMapper.treeToValue(jsonSchema, ToolParameters.Builder.class).build();
            toolSpecification = ToolSpecification.builder().name(methodName).description(methodDesc).parameters(toolParameters).build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Response<AiMessage> aiMessageResponse = LangChainChat.COMMON_CHAT_MODEL.generate(messages, toolSpecification);
        return aiMessageResponse.content();
    }

}
