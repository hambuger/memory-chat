package io.github.memorychat.audio;

import com.alibaba.fastjson.JSON;

import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.audio.transcription.AudioTranscription;
import org.springframework.ai.openai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.openai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static io.github.memorychat.constants.Constants.TEMPLATE;


/**
 * @author hamburger
 * @since 2024/6/17
 */
@Component
public class SpringAiAudio {


    @Autowired
    private OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;


    public String generateTextWithAudio(Resource audioFile) {
        OpenAiAudioTranscriptionOptions transcriptionOptions =
                OpenAiAudioTranscriptionOptions.builder().withResponseFormat(OpenAiAudioApi.TranscriptResponseFormat.TEXT).withTemperature(TEMPLATE.floatValue()).build();
        AudioTranscriptionPrompt transcriptionRequest = new AudioTranscriptionPrompt(audioFile, transcriptionOptions);
        AudioTranscriptionResponse response = openAiAudioTranscriptionModel.call(transcriptionRequest);
        System.out.println(JSON.toJSONString(response));
        return Optional.ofNullable(response).map(AudioTranscriptionResponse::getResult).map(AudioTranscription::getOutput).orElse(null);
    }

}
