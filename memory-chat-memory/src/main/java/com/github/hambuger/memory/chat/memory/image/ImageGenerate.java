package com.github.hambuger.memory.chat.memory.image;

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
public class ImageGenerate {

    @Value("${image.channel}")
    private String imageChannel;

    private static final Map<String, Function<ImageGenerateParam, String>> IMAGE_CHANNEL_MAP = new ConcurrentHashMap<>();


    public static void registerChannel(String key, Function<ImageGenerateParam, String> function) {
        IMAGE_CHANNEL_MAP.put(key, function);
    }


    @FunctionCallRegistry(functionDesc = "Generate pictures", scene = {ChatSceneEnum.NORMAL_USER, ChatSceneEnum.NORMAL_GROUP})
    public String generateImg(ImageGenerateParam query) {

        return IMAGE_CHANNEL_MAP.get(imageChannel).apply(query);

    }

}
