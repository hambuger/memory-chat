package com.github.hambuger.memory.chat.memory.token;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import dev.langchain4j.internal.Exceptions;
import org.springframework.ai.openai.api.OpenAiApi;

import java.util.Optional;
import java.util.function.Supplier;

public class TokenCalculation {

    private final Optional<Encoding> encoding;

    private String modelName;

    public TokenCalculation(String modelName) {
        this.modelName = modelName;
        this.encoding = Encodings.newLazyEncodingRegistry().getEncodingForModel(modelName);
    }

    public int getSpringAiMessageTokenCount(String text) {
        return this.encoding.orElseThrow(this.unknownModelException()).countTokensOrdinary(text);
    }

    private Supplier<IllegalArgumentException> unknownModelException() {
        return () -> Exceptions.illegalArgument("Model '%s' is unknown to jtokkit", new Object[]{this.modelName});
    }

    public int getSpringAiMessageTokenCount(OpenAiApi.ChatCompletionMessage message) {
        int tokenCount = 1;
        tokenCount += 4;
        if (OpenAiApi.ChatCompletionMessage.Role.SYSTEM.equals(message.role())) {
            tokenCount += getSpringAiMessageTokenCount(message.content());
        } else if (OpenAiApi.ChatCompletionMessage.Role.USER.equals(message.role())) {
            tokenCount += this.getUserMessageToken(message);
        } else if (OpenAiApi.ChatCompletionMessage.Role.ASSISTANT.equals(message.role())) {
            tokenCount += getAiMessageToken(message);
        } else if (OpenAiApi.ChatCompletionMessage.Role.TOOL.equals(message.role())) {
            tokenCount += getSpringAiMessageTokenCount(message.content());
        } else {
            throw new IllegalArgumentException("Unknown message type: " + message);
        }
        return tokenCount;
    }

    private int getAiMessageToken(OpenAiApi.ChatCompletionMessage aiMessage) {
        int tokenCount = 0;
        if (aiMessage.content() != null) {
            tokenCount += this.getSpringAiMessageTokenCount(aiMessage.content());
        }

//        if (aiMessage.toolExecutionRequests() != null) {
//            if (this.isOneOfLatestModels()) {
//                tokenCount += 6;
//            } else {
//                tokenCount += 3;
//            }
//
//            if (aiMessage.toolExecutionRequests().size() == 1) {
//                --tokenCount;
//                ToolExecutionRequest toolExecutionRequest = (ToolExecutionRequest)aiMessage.toolExecutionRequests().get(0);
//                tokenCount += this.estimateTokenCountInText(toolExecutionRequest.name()) * 2;
//                tokenCount += this.estimateTokenCountInText(toolExecutionRequest.arguments());
//            } else {
//                tokenCount += 15;
//                Iterator var8 = aiMessage.toolExecutionRequests().iterator();
//
//                while(var8.hasNext()) {
//                    ToolExecutionRequest toolExecutionRequest = (ToolExecutionRequest)var8.next();
//                    tokenCount += 7;
//                    tokenCount += this.estimateTokenCountInText(toolExecutionRequest.name());
//                    Map<?, ?> arguments = (Map) Json.fromJson(toolExecutionRequest.arguments(), Map.class);
//
//                    Map.Entry argument;
//                    for(Iterator var6 = arguments.entrySet().iterator(); var6.hasNext(); tokenCount += this.estimateTokenCountInText(argument.getValue().toString())) {
//                        argument = (Map.Entry)var6.next();
//                        tokenCount += 2;
//                        tokenCount += this.estimateTokenCountInText(argument.getKey().toString());
//                    }
//                }
//            }
//        }

        return tokenCount;
    }

    public int getUserMessageToken(OpenAiApi.ChatCompletionMessage message) {
        int tokenCount = 0;
//        Iterator var3 = message.rawContent();
//
//        while(var3.hasNext()) {
//            Content content = (Content)var3.next();
//            if (content instanceof TextContent) {
//                tokenCount += this.estimateTokenCountInText(((TextContent)content).text());
//            } else {
//                if (!(content instanceof ImageContent)) {
//                    throw Exceptions.illegalArgument("Unknown content type: " + content, new Object[0]);
//                }
//
//                tokenCount += 85;
//            }
//        }
//
//        if (userMessage.name() != null && !this.modelName.equals(OpenAiChatModelName.GPT_4_VISION_PREVIEW.toString())) {
//            tokenCount += this.extraTokensPerName();
//            tokenCount += this.estimateTokenCountInText(userMessage.name());
//        }

        return tokenCount;
    }

}
