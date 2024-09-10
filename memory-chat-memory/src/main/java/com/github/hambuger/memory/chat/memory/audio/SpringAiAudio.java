package com.github.hambuger.memory.chat.memory.audio;

import com.github.hambuger.memory.chat.memory.other.prompt.PromptFactory;
import com.github.hambuger.memory.chat.memory.other.util.VideoUtil;

import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.audio.transcription.AudioTranscription;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * @author hamburger
 * @since 2024/6/17
 */
@Component
public class SpringAiAudio {


    @Autowired
    private OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;

    @Autowired
    private VideoUtil videoUtil;

    @Value("${spring.ai.openai.temperature}")
    private Float temperature;

    @jakarta.annotation.Resource
    private PromptFactory promptFactory;


    public String generateTextWithAudio(Resource audioFile) {
        OpenAiAudioTranscriptionOptions transcriptionOptions =
                OpenAiAudioTranscriptionOptions.builder().withResponseFormat(OpenAiAudioApi.TranscriptResponseFormat.TEXT).withTemperature(temperature).withPrompt(promptFactory.getAudioPrompt()).build();
        AudioTranscriptionPrompt transcriptionRequest = new AudioTranscriptionPrompt(audioFile, transcriptionOptions);
        AudioTranscriptionResponse response = openAiAudioTranscriptionModel.call(transcriptionRequest);
        return Optional.ofNullable(response).map(AudioTranscriptionResponse::getResult).map(AudioTranscription::getOutput).orElse(null);
    }


    public List<String> generateTextFromVideo(String videoFilePath) {
        List<String> pathAndText = new ArrayList<>();
        String audioFilePath = videoUtil.extractVideoAudio(videoFilePath);
        if (audioFilePath == null) {
            return pathAndText;
        }
        pathAndText.add(audioFilePath);
        String text = generateTextWithAudio(new FileSystemResource(audioFilePath));
        pathAndText.add(text);
        return pathAndText;

    }

}
