package com.github.hambuger.memory.chat.memory.chat;

import com.github.hambuger.memory.chat.memory.constants.LangChainConstants;
import com.github.hambuger.memory.chat.memory.constants.MemoryChatConstants;
import com.github.hambuger.memory.chat.memory.util.CallFunctionRegistryFactory;

import org.apache.commons.collections4.CollectionUtils;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;

import static com.github.hambuger.memory.chat.memory.constants.MemoryChatConstants.REPLY_MESSAGE_FUNCTION_NAME;


/**
 * @author hamburger
 * @since 2024/6/13
 */
public class LangChainChat {

    public static OpenAiChatModel FORMAT_JSON_MODEL =
            OpenAiChatModel.builder().proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(LangChainConstants.LOCAL, LangChainConstants.PROXY_PORT))).baseUrl(LangChainConstants.API_HOST).apiKey(LangChainConstants.API_KEY).temperature(LangChainConstants.TEMPLATE).logRequests(true).logResponses(true).modelName(LangChainConstants.MODEL_NAME).responseFormat(MemoryChatConstants.JSON_OBJECT).maxRetries(MemoryChatConstants.MAX_RETRIES_NO).build();

    public static OpenAiChatModel COMMON_CHAT_MODEL =
            OpenAiChatModel.builder().maxRetries(MemoryChatConstants.MAX_RETRIES_NO).proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(LangChainConstants.LOCAL, LangChainConstants.PROXY_PORT))).baseUrl(LangChainConstants.API_HOST).apiKey(LangChainConstants.API_KEY).temperature(LangChainConstants.TEMPLATE).logRequests(true).logResponses(true).modelName(LangChainConstants.MODEL_NAME).build();


    public static String generateJsonWithSingleMsgAndPrompt(String msg) {
        return FORMAT_JSON_MODEL.generate(msg);
    }


    public static Response<AiMessage> generateMsgWithMsgList(List<ChatMessage> messageList) {

        return COMMON_CHAT_MODEL.generate(messageList);
    }


    public static Response<AiMessage> generateMsgWithMsgListAndFunctions(List<ChatMessage> messageList) {
        List<ToolSpecification> toolSpecifications = CallFunctionRegistryFactory.getAllFunctionCall(false);
        if (CollectionUtils.isEmpty(toolSpecifications)) {
            return generateMsgWithMsgList(messageList);
        }
        Response<AiMessage> response = COMMON_CHAT_MODEL.generate(messageList, toolSpecifications);
        AiMessage aiMessage = response.content();
        if (aiMessage == null || CollectionUtils.isEmpty(aiMessage.toolExecutionRequests()) || aiMessage.toolExecutionRequests().stream().anyMatch(tool -> tool.name().equals(REPLY_MESSAGE_FUNCTION_NAME))) {
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
