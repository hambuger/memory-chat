package com.github.hambuger.memory.chat.memory.image;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.hambuger.memory.chat.memory.chat.SpringAiChat;
import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.image.utils.Credentials;
import com.github.hambuger.memory.chat.memory.image.utils.Signer;
import com.github.hambuger.memory.chat.memory.other.functionCall.aop.FunctionCallRegistry;
import com.github.hambuger.memory.chat.memory.other.prompt.PromptFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;


@Slf4j
@Component
public class DoubaoAiImage {

    @Value("${doubao.accessKey}")
    private String accessKey;

    @Value("${doubao.secretAccessKey}")
    private String secretAccessKey;

    @Value("${temp.path}")
    private String tempPath;

    @Resource
    private PromptFactory promptFactory;

    @Resource
    private SpringAiChat springAiChat;

    private static final String URL = "https://visual.volcengineapi.com?Action=HighAesSmartDrawing&Version=2022-08-31";

    @FunctionCallRegistry(functionDesc = "Generate pictures", scene = {ChatSceneEnum.NORMAL_USER, ChatSceneEnum.NORMAL_GROUP})
    public String generateImg(SpringAiImage.ImageGenerateParam param) {
        try {
            String imageGeneratePrompt = promptFactory.getImageGeneratePrompt();
            List<OpenAiApi.ChatCompletionMessage> messageList = new ArrayList<>();
            messageList.add(new OpenAiApi.ChatCompletionMessage(imageGeneratePrompt, OpenAiApi.ChatCompletionMessage.Role.SYSTEM));
            messageList.add(new OpenAiApi.ChatCompletionMessage(param.getGenerateText(), OpenAiApi.ChatCompletionMessage.Role.USER));
            OpenAiApi.ChatCompletion chatCompletion = springAiChat.generateMsgWithMsgList(messageList, false);
            String prompt = chatCompletion.choices().get(0).message().content();
            Credentials credentials = new Credentials();
            credentials.setAccessKeyID(accessKey);
            credentials.setSecretAccessKey(secretAccessKey);
            credentials.setRegion("cn-north-1");
            credentials.setService("cv");

            Signer signer = new Signer();

            CloseableHttpClient httpClient = HttpClients.createDefault();

            HttpPost request = new HttpPost();

            request.setURI(new URI(URL));


            request.addHeader(HttpHeaders.USER_AGENT, "volc-sdk-java/v1.0.0");

            JSONObject json = new JSONObject();
            json.put("req_key", "high_aes_general_v20");
            json.put("prompt", prompt);
            json.put("model_version", "general_v2.0");
            json.put("seed", -1);
            json.put("ddim_steps", 16);
            json.put("width", 512);
            json.put("height", 512);
            json.put("use_rephraser", false);
            json.put("use_sr", true);
            json.put("return_url", false);
            StringEntity entity1 = new StringEntity(json.toString());
            entity1.setContentType("application/json");
            request.setEntity(entity1);

            signer.sign(request, credentials);

            CloseableHttpResponse response = httpClient.execute(request);  // 200

            HttpEntity entity = response.getEntity();
            String imagePath = null;
            if (entity != null) {
                String result = EntityUtils.toString(entity);
                JSONObject rootNode = JSON.parseObject(result);
                JSONObject dataNode = rootNode.getJSONObject("data");
                if (dataNode != null) {
                    String[] binaryDataArray = dataNode.getObject("binary_data_base64", String[].class);
                    if (binaryDataArray != null && binaryDataArray.length > 0) {
                        String base64Image = binaryDataArray[0];
                        byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                        imagePath = tempPath + File.separator + UUID.randomUUID().toString().replace("-", "") + ".png";
                        try (FileOutputStream imageOutFile = new FileOutputStream(imagePath)) {
                            imageOutFile.write(imageBytes);
                        } catch (IOException e) {
                            log.error("write image", e);
                        }
                    } else {
                        log.error("binary_data_base64 is empty");
                    }
                }
            }
            response.close();
            httpClient.close();
            return imagePath;
        } catch (Exception e) {
            log.error("generateImg error", e);
        }
        return null;
    }
}

