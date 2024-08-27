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
 * 实时识别麦克风输入示例
 */
public class TencentAsr {

    static Logger logger = LoggerFactory.getLogger(TencentAsr.class);

    //SpeechClient应用全局创建一个即可,生命周期可和整个应用保持一致
    static SpeechClient proxy = new SpeechClient(AsrConstant.DEFAULT_RT_REQ_URL);


    public static void main(String[] args) {
        //在腾讯云控制台[账号信息](https://console.cloud.tencent.com/developer)页面查看账号APPID，[访问管理](https://console.cloud.tencent.com/cam/capi)页面获取 SecretID 和 SecretKey 。
        //todo 在使用该接口前，需要开通该服务，并请将下面appId、secretId、secretKey替换为自己账号信息。
        String appId = "1255828410";
        String secretId = "AKID41oS7hEuX2MTMju0DPVXbq01tb8at581";
        String secretKey = "rbBjEe1KXfhYDhOxOjy78GQSSPCrSNWk";
        process(appId, secretId, secretKey);
        proxy.shutdown();
    }


    public static void process(String appId, String secretId, String secretKey) {
        Credential credential = new Credential(appId, secretId, secretKey);
        SpeechRecognizerRequest request = SpeechRecognizerRequest.init();
        request.setEngineModelType("8k_zh");
        request.setVoiceFormat(1);
        request.setVoiceId(UUID.randomUUID().toString()); // voice_id为请求标识，需要保持全局唯一（推荐使用 uuid），遇到问题需要提供该值方便服务端排查
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
            // 配置麦克风输入格式
            AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                logger.error("麦克风不支持该格式");
                return;
            }

            // 打开麦克风并开始捕获音频
            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(format);
            targetDataLine.start();

            speechRecognizer = new SpeechRecognizer(proxy, credential, request, listener);
            speechRecognizer.start();
            logger.info("speechRecognizer 已启动");

            byte[] buffer = new byte[640];
            while (true) { // 可根据需求控制循环条件
                int bytesRead = targetDataLine.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    speechRecognizer.write(buffer);
                }
                // 可根据需求调整线程休眠时间来模拟实时传输的速度
                Thread.sleep(20);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            if (speechRecognizer != null) {
                speechRecognizer.close(); //关闭连接
            }
            if (targetDataLine != null) {
                targetDataLine.stop();
                targetDataLine.close();
            }
        }
    }
}
