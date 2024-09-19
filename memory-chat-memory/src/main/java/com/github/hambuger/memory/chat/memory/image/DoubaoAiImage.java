package com.github.hambuger.memory.chat.memory.image;

import com.alibaba.fastjson.JSONObject;
import com.github.hambuger.memory.chat.memory.image.utils.Credentials;
import com.github.hambuger.memory.chat.memory.image.utils.Signer;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
public class DoubaoAiImage {


    private static final String URL = "https://visual.volcengineapi.com?Action=HighAesSmartDrawing&Version=2022-08-31";

    public void generateImg(String prompt){
        Credentials credentials = new Credentials();
        credentials.setAccessKeyID("xxxx");
        credentials.setSecretAccessKey("xxxx==");
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
        if (entity != null) {
            String result = EntityUtils.toString(entity);
            JSONObject rootNode = JSON.parseObject(result);
            JSONObject dataNode = rootNode.getJSONObject("data");
            if (dataNode != null) {
                String[] binaryDataArray = dataNode.getObject("binary_data_base64", String[].class);
                if (binaryDataArray != null && binaryDataArray.length > 0) {
                    String base64Image = binaryDataArray[0];
                    byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                    try (FileOutputStream imageOutFile = new FileOutputStream(new File("image.png"))) {
                        imageOutFile.write(imageBytes);
                        System.out.println("图片已成功保存为 image.png");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("binary_data_base64 数组为空或不存在");
                }
            }
        }

        response.close();
        httpClient.close();
    }
}

