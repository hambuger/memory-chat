package com.github.hambuger.memory.chat.memory.emoji;

import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.other.functionCall.aop.FunctionCallRegistry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;


/**
 * @author hanjiabao
 * @since 2024/9/20
 */
@Slf4j
@Component
public class EmojiManager {

    @Value("${emoji.channel}")
    private String emojiChannel;

    private static final Map<String, Function<EmoticonPictureQuery, String>> EMOJI_CHANNEL_MAP = new ConcurrentHashMap<>();


    public static void registerChannel(String key, Function<EmoticonPictureQuery, String> function) {
        EMOJI_CHANNEL_MAP.put(key, function);
    }

    @FunctionCallRegistry(functionDesc = "Search for emoticon pictures and return the picture URL", scene = {ChatSceneEnum.NORMAL_GROUP, ChatSceneEnum.NORMAL_USER})
    public String searchEmoticonPhoto(EmoticonPictureQuery query) {

        return EMOJI_CHANNEL_MAP.get(emojiChannel).apply(query);

    }

}
