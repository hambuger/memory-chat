package com.github.hambuger.memory.chat.memory.audio;

import com.google.gson.Gson;

import com.tencent.asrv2.AsrConstant;
import com.tencent.asrv2.SpeechRecognizer;
import com.tencent.asrv2.SpeechRecognizerListener;
import com.tencent.asrv2.SpeechRecognizerRequest;
import com.tencent.asrv2.SpeechRecognizerResponse;
import com.tencent.core.ws.Credential;
import com.tencent.core.ws.SpeechClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;


/**
 * Examples of real-time recognition of microphone input
 */
public class TencentAsr {

    static Logger logger = LoggerFactory.getLogger(TencentAsr.class);

    static SpeechClient proxy = new SpeechClient(AsrConstant.DEFAULT_RT_REQ_URL);


    public static void main(String[] args) {
        //todo Before using this API, you need to activate the service and replace the following appId, secretId, and secretKey with your account information.
        String appId = "xxx";
        String secretId = "xxx";
        String secretKey = "xxx";
        process(appId, secretId, secretKey);
        proxy.shutdown();
    }


    public static void process(String appId, String secretId, String secretKey) {
        Credential credential = new Credential(appId, secretId, secretKey);
        SpeechRecognizerRequest request = SpeechRecognizerRequest.init();
        request.setEngineModelType("8k_zh");
        request.setVoiceFormat(1);
        request.setVoiceId(UUID.randomUUID().toString());
        logger.debug("voice_id:{}", request.getVoiceId());

        SpeechRecognizerListener listener = new SpeechRecognizerListener() {
            @Override
            public void onRecognitionStart(SpeechRecognizerResponse response) {
                logger.info("{} voice_id:{},{}", "onRecognitionStart", response.getVoiceId(), new Gson().toJson(response));
            }


            @Override
            public void onSentenceBegin(SpeechRecognizerResponse response) {
                logger.info("{} voice_id:{},{}", "onSentenceBegin", response.getVoiceId(), new Gson().toJson(response));
            }


            @Override
            public void onRecognitionResultChange(SpeechRecognizerResponse response) {
                logger.info("{} voice_id:{},{}", "onRecognitionResultChange", response.getVoiceId(), new Gson().toJson(response));
            }


            @Override
            public void onSentenceEnd(SpeechRecognizerResponse response) {
                logger.info("{} voice_id:{},{}", "onSentenceEnd", response.getVoiceId(), new Gson().toJson(response));
            }


            @Override
            public void onRecognitionComplete(SpeechRecognizerResponse response) {
                logger.info("{} voice_id:{},{}", "onRecognitionComplete", response.getVoiceId(), new Gson().toJson(response));
            }


            @Override
            public void onFail(SpeechRecognizerResponse response) {
                logger.info("{} voice_id:{},{}", "onFail", response.getVoiceId(), new Gson().toJson(response));
            }


            @Override
            public void onMessage(SpeechRecognizerResponse response) {
                logger.info("{} voice_id:{},{}", "onMessage", response.getVoiceId(), new Gson().toJson(response));
            }
        };

        SpeechRecognizer speechRecognizer = null;
        TargetDataLine targetDataLine = null;
        try {
            // Configure the microphone input format
            AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                logger.error("The microphone does not support this format");
                return;
            }

            // Turn on the microphone and start capturing audio
            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(format);
            targetDataLine.start();

            speechRecognizer = new SpeechRecognizer(proxy, credential, request, listener);
            speechRecognizer.start();
            logger.info("speechRecognizer Started");

            byte[] buffer = new byte[640];
            while (true) {
                int bytesRead = targetDataLine.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    speechRecognizer.write(buffer);
                }
                Thread.sleep(20);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            if (speechRecognizer != null) {
                speechRecognizer.close();
            }
            if (targetDataLine != null) {
                targetDataLine.stop();
                targetDataLine.close();
            }
        }
    }
}
