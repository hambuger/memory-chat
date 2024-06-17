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


/**
 * @author hamburger
 * @since 2024/6/13
 */
public class LangChainChat {

    public static String generateJsonWithSingleMsgAndPrompt(String msg) {

        OpenAiChatModel model =
                OpenAiChatModel.builder().proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(Constants.LOCAL, Constants.PROXY_PORT))).baseUrl(Constants.API_HOST).apiKey(Constants.API_KEY).temperature(0.0).logRequests(true).logResponses(true).modelName(Constants.MODEL_NAME).responseFormat("json_object").maxRetries(3).build();
        String json = model.generate(msg);
        return json;
    }


    public static Response<AiMessage> generateMsgWithMsgList(List<ChatMessage> messageList) {

        OpenAiChatModel model =
                OpenAiChatModel.builder().maxRetries(3).proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(Constants.LOCAL, Constants.PROXY_PORT))).baseUrl(Constants.API_HOST).apiKey(Constants.API_KEY).temperature(0.0).logRequests(true).logResponses(true).modelName(Constants.MODEL_NAME).build();
        return model.generate(messageList);
    }


    public static Response<AiMessage> generateMsgWithMsgListAndFunctions(List<ChatMessage> messageList) {
        List<ToolSpecification> toolSpecifications = CallFunctionRegistryFactory.getAllFunctionCall();
        if (CollectionUtils.isEmpty(toolSpecifications)) {
            return generateMsgWithMsgList(messageList);
        }
        OpenAiChatModel model =
                OpenAiChatModel.builder().maxRetries(3).proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(Constants.LOCAL, Constants.PROXY_PORT))).baseUrl(Constants.API_HOST).apiKey(Constants.API_KEY).temperature(0.0).logRequests(true).logResponses(true).modelName(Constants.MODEL_NAME).build();

        Response<AiMessage> response = model.generate(messageList, toolSpecifications);
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
