package com.github.hambuger.memory.chat.memory.audio;

import com.alibaba.fastjson.JSON;
import com.github.hambuger.memory.chat.memory.constants.Constants;
import com.github.hambuger.memory.chat.memory.util.VideoUtil;

import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.audio.transcription.AudioTranscription;
import org.springframework.ai.openai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.openai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

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


    public String generateTextWithAudio(Resource audioFile) {
        OpenAiAudioTranscriptionOptions transcriptionOptions =
                OpenAiAudioTranscriptionOptions.builder().withResponseFormat(OpenAiAudioApi.TranscriptResponseFormat.TEXT).withTemperature(Constants.TEMPLATE.floatValue()).build();
        AudioTranscriptionPrompt transcriptionRequest = new AudioTranscriptionPrompt(audioFile, transcriptionOptions);
        AudioTranscriptionResponse response = openAiAudioTranscriptionModel.call(transcriptionRequest);
        return Optional.ofNullable(response).map(AudioTranscriptionResponse::getResult).map(AudioTranscription::getOutput).orElse(null);
    }


    public String generateTextFromVideo(String videoFilePath) {

        String audioFilePath = videoUtil.extractVideoAudio(videoFilePath);
        if (audioFilePath == null) {
            return null;
        }
        return generateTextWithAudio(new FileSystemResource(audioFilePath));

    }

}
