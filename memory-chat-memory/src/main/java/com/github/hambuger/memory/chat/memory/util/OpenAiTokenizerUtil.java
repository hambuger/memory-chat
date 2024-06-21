package com.github.hambuger.memory.chat.memory.util;

import com.github.hambuger.memory.chat.memory.constants.Constants;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.openai.OpenAiTokenizer;


/**
 * @author hamburger
 * @since 2024/6/14
 */
public class OpenAiTokenizerUtil {

    private static OpenAiTokenizer openAiTokenizer = new OpenAiTokenizer(Constants.MODEL_NAME);


    public static int getTextToken(String text) {
        return openAiTokenizer.estimateTokenCountInText(text);
    }


    public static int getMessageToken(ChatMessage message) {
        return openAiTokenizer.estimateTokenCountInMessage(message);
    }

}
