package com.github.hambuger.memory.chat.memory.audio;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.concurrent.CountDownLatch;

import javax.annotation.Nullable;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;

import cn.xfyun.api.RtasrClient;
import cn.xfyun.model.response.rtasr.RtasrResponse;
import cn.xfyun.service.rta.AbstractRtasrWebSocketListener;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.WebSocket;
import okio.ByteString;


/**
 * 实时麦克风语音转写
 */
@Slf4j
public class XunfeiAsr {

    // 音频格式配置
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(16000, 16, 1, true, false);


    public static void main(String[] args) throws Exception {
        send();
    }


    public static void send() throws InterruptedException {
        // 初始化讯飞RTASR客户端
        RtasrClient rtasrClient = new RtasrClient.Builder().signature("2f6fc2b9", "8eb0429e8b038d2fc3db9baad4994cca").build();
        CountDownLatch latch = new CountDownLatch(1);
        WebSocket webSocket = rtasrClient.newWebSocket(new AbstractRtasrWebSocketListener() {
            @Override
            public void onSuccess(WebSocket webSocket, String text) {
                RtasrResponse response = JSONObject.parseObject(text, RtasrResponse.class);
                log.info(getContent(response.getData()));
            }


            @Override
            public void onFail(WebSocket webSocket, Throwable t, @Nullable Response response) {
                latch.countDown();
                System.exit(0);
            }


            @Override
            public void onBusinessFail(WebSocket webSocket, String text) {
                log.info(text);
                latch.countDown();
                System.exit(0);
            }


            @Override
            public void onClosed() {
                latch.countDown();
                System.exit(0);
            }
        });

        // 捕获麦克风音频数据并发送
        try (TargetDataLine microphone = AudioSystem.getTargetDataLine(AUDIO_FORMAT)) {
            microphone.open(AUDIO_FORMAT);
            microphone.start();
            byte[] buffer = new byte[1280];
            long lastTs = 0;
            while (true) {
                int bytesRead = microphone.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    if (bytesRead < buffer.length) {
                        webSocket.send(ByteString.of(buffer, 0, bytesRead));
                        break;
                    }

                    long curTs = System.currentTimeMillis();
                    if (lastTs != 0) {
                        long s = curTs - lastTs;
                        if (s < 40) {
                            log.info("error time interval: " + s + " ms");
                        }
                    }
                    webSocket.send(ByteString.of(buffer));
                    lastTs = curTs;
                    Thread.sleep(40);
                }
            }
            // 发送结束标识
            rtasrClient.sendEnd();
        } catch (Exception e) {
            log.error("error", e);
        }

        latch.await();
    }


    // 把转写结果解析为句子
    public static String getContent(String message) {
        try {
            JSONObject data = JSON.parseObject(message);
            if (data.containsKey("cn") && data.getJSONObject("cn").containsKey("st")) {
                JSONObject st = data.getJSONObject("cn").getJSONObject("st");
                if (st.containsKey("rt")) {
                    JSONArray rtArr = st.getJSONArray("rt");
                    StringBuilder resultText = new StringBuilder();
                    int wb = 0;

                    for (int i = 0; i < rtArr.size(); i++) {
                        JSONObject rtObj = rtArr.getJSONObject(i);
                        JSONArray wsArr = rtObj.getJSONArray("ws");
                        for (int j = 0; j < wsArr.size(); j++) {
                            JSONObject wsObj = wsArr.getJSONObject(j);
                            resultText.append(wsObj.getJSONArray("cw").getJSONObject(0).getString("w"));
                        }
                        wb = wsArr.getJSONObject(0).getInteger("wb");
                    }

                    if (wb == 0) {
                        // 中间结果
                        return "中间结果: " + resultText;
                    }else {
                        // 最终结果
                        return "最终结果: " + resultText;
                    }
                }
            }
        } catch (Exception e) {
            log.error("error", e);
        }
        return null;
    }
}
