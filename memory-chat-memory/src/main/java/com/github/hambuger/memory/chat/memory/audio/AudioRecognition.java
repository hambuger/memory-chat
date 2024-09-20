package com.github.hambuger.memory.chat.memory.audio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
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
public class AudioRecognition {

    @Value("${audio.model}")
    private String audioModel;

    private static final Map<String, Function<Resource, String>> AUDIO_RECOGNITION_MAP = new ConcurrentHashMap<>();


    public static void registerRecognition(String modelKey, Function<Resource, String> function) {
        AUDIO_RECOGNITION_MAP.put(modelKey, function);
    }


    public String recognitionAudio(Resource audioFile) {

        return AUDIO_RECOGNITION_MAP.get(audioModel).apply(audioFile);

    }

}
