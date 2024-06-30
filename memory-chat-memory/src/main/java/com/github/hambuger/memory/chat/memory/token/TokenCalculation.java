package com.github.hambuger.memory.chat.memory.token;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.openai.api.OpenAiApi;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import dev.langchain4j.internal.Exceptions;
import dev.langchain4j.internal.Json;


public class TokenCalculation {

    private final Optional<Encoding> encoding;

    private String modelName;


    public TokenCalculation(String modelName) {
        this.modelName = modelName;
        this.encoding = Encodings.newLazyEncodingRegistry().getEncodingForModel(modelName);
    }


    public int getMessageTextTokenCount(String text) {
        return this.encoding.orElseThrow(this.unknownModelException()).countTokensOrdinary(text);
    }


    private Supplier<IllegalArgumentException> unknownModelException() {
        return () -> Exceptions.illegalArgument("Model '%s' is unknown to jtokkit", new Object[]{this.modelName});
    }


    public int getMessageTextTokenCount(OpenAiApi.ChatCompletionMessage message) {
        int tokenCount = 1;
        tokenCount += 5;
        if (OpenAiApi.ChatCompletionMessage.Role.SYSTEM.equals(message.role())) {
            tokenCount += getMessageTextTokenCount(message.content());
        }else if (OpenAiApi.ChatCompletionMessage.Role.USER.equals(message.role())) {
            tokenCount += this.getUserMessageToken(message);
        }else if (OpenAiApi.ChatCompletionMessage.Role.ASSISTANT.equals(message.role())) {
            tokenCount += getAiMessageToken(message);
        }else if (OpenAiApi.ChatCompletionMessage.Role.TOOL.equals(message.role())) {
            tokenCount += getMessageTextTokenCount(message.content());
        }else {
            throw new IllegalArgumentException("Unknown message type: " + message);
        }
        return tokenCount;
    }


    private int getAiMessageToken(OpenAiApi.ChatCompletionMessage aiMessage) {
        int tokenCount = 0;
        if (aiMessage.toolCalls() != null) {
            tokenCount += 6;

            if (aiMessage.toolCalls().size() == 1) {
                --tokenCount;
                OpenAiApi.ChatCompletionMessage.ToolCall toolExecutionRequest = aiMessage.toolCalls().get(0);
                tokenCount += this.getMessageTextTokenCount(toolExecutionRequest.function().name()) * 2;
                tokenCount += this.getMessageTextTokenCount(toolExecutionRequest.function().arguments());
            }else {
                tokenCount += 15;
                Iterator toolInfo = aiMessage.toolCalls().iterator();

                while (toolInfo.hasNext()) {
                    OpenAiApi.ChatCompletionMessage.ToolCall toolExecutionRequest = (OpenAiApi.ChatCompletionMessage.ToolCall) toolInfo.next();
                    tokenCount += 7;
                    tokenCount += this.getMessageTextTokenCount(toolExecutionRequest.function().name());
                    Map<?, ?> arguments = (Map) Json.fromJson(toolExecutionRequest.function().arguments(), Map.class);

                    Map.Entry argument;
                    for (Iterator var6 = arguments.entrySet().iterator(); var6.hasNext(); tokenCount += this.getMessageTextTokenCount(argument.getValue().toString())) {
                        argument = (Map.Entry) var6.next();
                        tokenCount += 2;
                        tokenCount += this.getMessageTextTokenCount(argument.getKey().toString());
                    }
                }
            }
        }
        return tokenCount;
    }


    public int getUserMessageToken(OpenAiApi.ChatCompletionMessage message) {
        int tokenCount = 0;
        if (StringUtils.isNotBlank(message.name())) {
            tokenCount += getMessageTextTokenCount(message.name());
        }
        Object msgObj = message.rawContent();
        if (msgObj == null) {
            return tokenCount;
        }
        if (msgObj instanceof String) {
            String text = msgObj.toString();
            tokenCount += this.getMessageTextTokenCount(text);
            return tokenCount;
        }else if (msgObj instanceof OpenAiApi.ChatCompletionMessage.MediaContent) {
            tokenCount += countImageToken((OpenAiApi.ChatCompletionMessage.MediaContent) msgObj);
            return tokenCount;
        }
        //数组判空
        if (msgObj.getClass().isArray()) {
            int len = Array.getLength(msgObj);
            Object[] obj = new Object[len];
            for (int i = 0; i < len; i++) {
                Object o = Array.get(obj, i);
                if (o instanceof String) {
                    tokenCount += this.getMessageTextTokenCount(o.toString());
                }else if (o instanceof OpenAiApi.ChatCompletionMessage.MediaContent) {
                    tokenCount += countImageToken((OpenAiApi.ChatCompletionMessage.MediaContent) o);
                }
            }
        }
        return tokenCount;
    }


    private int countImageToken(OpenAiApi.ChatCompletionMessage.MediaContent msgObj) {
        return 85;
    }
}
