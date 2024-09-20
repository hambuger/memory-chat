package com.github.hambuger.memory.chat.memory.audio;

import com.github.hambuger.memory.chat.memory.other.util.VideoUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
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

    @Autowired
    private VideoUtil videoUtil;

    private static final Map<String, Function<Resource, String>> AUDIO_RECOGNITION_MAP = new ConcurrentHashMap<>();


    public static void registerRecognition(String modelKey, Function<Resource, String> function) {
        AUDIO_RECOGNITION_MAP.put(modelKey, function);
    }


    public List<String> generateTextFromVideo(String videoFilePath) {
        List<String> pathAndText = new ArrayList<>();
        String audioFilePath = videoUtil.extractVideoAudio(videoFilePath);
        if (audioFilePath == null) {
            return pathAndText;
        }
        pathAndText.add(audioFilePath);
        String text = recognitionAudio(new FileSystemResource(audioFilePath));
        pathAndText.add(text);
        return pathAndText;
    }

    public String recognitionAudio(Resource audioFile) {

        return AUDIO_RECOGNITION_MAP.get(audioModel).apply(audioFile);

    }

}
