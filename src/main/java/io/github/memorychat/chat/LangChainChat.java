package io.github.memorychat.chat;

import org.apache.commons.collections4.CollectionUtils;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import io.github.memorychat.constants.Constants;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import io.github.memorychat.util.CallFunctionRegistryFactory;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;

import static io.github.memorychat.constants.Constants.JSON_OBJECT;
import static io.github.memorychat.constants.Constants.MAX_RETRIES_NO;
import static io.github.memorychat.constants.Constants.TEMPLATE;


/**
 * @author hamburger
 * @since 2024/6/13
 */
public class LangChainChat {

    public static OpenAiChatModel FORMAT_JSON_MODEL =
            OpenAiChatModel.builder().proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(Constants.LOCAL, Constants.PROXY_PORT))).baseUrl(Constants.API_HOST).apiKey(Constants.API_KEY).temperature(TEMPLATE).logRequests(true).logResponses(true).modelName(Constants.MODEL_NAME).responseFormat(JSON_OBJECT).maxRetries(MAX_RETRIES_NO).build();

    public static OpenAiChatModel COMMON_CHAT_MODEL =
            OpenAiChatModel.builder().maxRetries(MAX_RETRIES_NO).proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(Constants.LOCAL, Constants.PROXY_PORT))).baseUrl(Constants.API_HOST).apiKey(Constants.API_KEY).temperature(TEMPLATE).logRequests(true).logResponses(true).modelName(Constants.MODEL_NAME).build();


    public static String generateJsonWithSingleMsgAndPrompt(String msg) {
        return FORMAT_JSON_MODEL.generate(msg);
    }


    public static Response<AiMessage> generateMsgWithMsgList(List<ChatMessage> messageList) {

        return COMMON_CHAT_MODEL.generate(messageList);
    }


    public static Response<AiMessage> generateMsgWithMsgListAndFunctions(List<ChatMessage> messageList) {
        List<ToolSpecification> toolSpecifications = CallFunctionRegistryFactory.getAllFunctionCall();
        if (CollectionUtils.isEmpty(toolSpecifications)) {
            return generateMsgWithMsgList(messageList);
        }
        Response<AiMessage> response = COMMON_CHAT_MODEL.generate(messageList, toolSpecifications);
        AiMessage aiMessage = response.content();
        if (aiMessage == null || CollectionUtils.isEmpty(aiMessage.toolExecutionRequests())) {
            return response;
        }
        List<ToolExecutionResultMessage> executionResultMessages = new ArrayList<>();
        for (ToolExecutionRequest toolExecution : aiMessage.toolExecutionRequests()) {
            executionResultMessages.add(ToolExecutionResultMessage.from(toolExecution, CallFunctionRegistryFactory.executeFunctionResult(toolExecution.name(), toolExecution.arguments())));
        }
        messageList.add(aiMessage);
        messageList.addAll(executionResultMessages);
        return generateMsgWithMsgListAndFunctions(messageList);
    }

}
