package io.github.memorychat.functionCall;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;

import io.github.memorychat.constants.Constants;
import dev.langchain4j.agent.tool.ToolParameters;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.openai.OpenAiChatModel;
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

        OpenAiChatModel model = OpenAiChatModel.builder().baseUrl(Constants.API_HOST).apiKey(Constants.API_KEY).proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(Constants.LOCAL,
                Constants.PROXY_PORT))).modelName(Constants.MODEL_NAME).logRequests(true).logResponses(true).build();
        ToolSpecification toolSpecification = null;
        try {
            ToolParameters toolParameters = objectMapper.treeToValue(jsonSchema, ToolParameters.Builder.class).build();
            toolSpecification = ToolSpecification.builder().name(methodName).description(methodDesc).parameters(toolParameters).build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Response<AiMessage> aiMessageResponse = model.generate(messages, toolSpecification);
        return aiMessageResponse.content();
    }

}
