package com.github.hambuger.memory.chat.memory.portrait;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.hambuger.memory.chat.memory.chat.SpringAiChat;
import com.github.hambuger.memory.chat.memory.other.prompt.PromptFactory;
import com.github.hambuger.memory.chat.memory.other.util.RedisUtil;
import com.github.hambuger.memory.chat.memory.other.util.UserInfoUtil;

import org.apache.poi.util.StringUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;


/**
 * @author hamburger
 * @since 2024/9/2
 */
@Slf4j
@Component
public class PortraitGenerate {

    @Value("${spring.ai.openai.api-key}")
    private String API_KEY;

    @Value("${spring.ai.openai.base-url}")
    private String OPENAI_URL;

    @Value("${spring.ai.openai.fine-tuning.model}")
    private String FINE_TUNING_MODEL;

    @Resource
    private SpringAiChat springAiChat;

    @Resource
    private PromptFactory promptFactory;

    @Value("${temp.path}")
    private String tempPath;

    @Resource
    private RedisUtil redisUtil;

    public static final String CUSTOM_MODEL_KEY = "%s:custom:model";

    private static final RestTemplate restTemplate = new RestTemplate();

    public String getCustomModel() {
        return redisUtil.getString(String.format(CUSTOM_MODEL_KEY, UserInfoUtil.getUser()));
    }

    @Data
    public static class ConversationExampleResult {

        @JsonPropertyDescription("生成的对话示例集合")
        @JsonProperty(required = true)
        private List<ConversationExample> exampleList;

        @Data
        public static class ConversationExample {

            @JsonPropertyDescription("对话的前一句，不包含说话人名")
            @JsonProperty(required = true)
            private String previousConversation;

            @JsonPropertyDescription("角色的回复，不包含说话人名")
            @JsonProperty(required = true)
            private String replyContent;

        }
    }

    public static void writeListToFile(List<String> list, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (String line : list) {
                writer.write(line);
                writer.newLine();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void generateCustomChatModel(String owner, String portraitPrompt) {
        if (StringUtil.isBlank(owner)) {
            return;
        }
        String exampleResults = springAiChat.generateJsonWithSingleMsgAndPrompt(promptFactory.getGenerateCustomChatModelPrompt(), portraitPrompt, ConversationExampleResult.class);
        ConversationExampleResult exampleResult = JSON.parseObject(exampleResults, ConversationExampleResult.class);
        if (exampleResult == null || CollectionUtils.isEmpty(exampleResult.getExampleList())) {
            return;
        }
        String dataPath = tempPath + File.separator + UUID.randomUUID() + "_data.jsonl";
        List<String> list =
                exampleResult.getExampleList().stream().map(example -> String.format("{\"messages\": [{\"role\": \"user\", \"content\": \"%s\"}, {\"role\": \"assistant\", \"content\": " + "\"%s" +
                        "\"}]}", example.getPreviousConversation(), example.getReplyContent())).toList();
        writeListToFile(list, dataPath);
        String model = getCustomChatModel(dataPath);
        if (StringUtil.isNotBlank(model)) {
            redisUtil.setString(String.format(CUSTOM_MODEL_KEY, owner), model);
        }
    }



    public String getCustomChatModel(String path) {
        String fileId = uploadFile(path);

        String fineTuningJobId = createFineTuningJob(fileId, FINE_TUNING_MODEL);

        return checkFineTuningJobStatus(fineTuningJobId);
    }


    public String uploadFile(String filePath) {
        File file = Paths.get(filePath).toFile();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(API_KEY);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(file));
        body.add("purpose", "fine-tune");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(OPENAI_URL + "/v1/files", HttpMethod.POST, requestEntity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            Map<String, Object> jsonResponse = parseJson(response.getBody());
            if (jsonResponse != null) {
                return jsonResponse.get("id").toString();
            }else {
                return null;
            }
        }else {
            throw new RuntimeException("Failed to upload file: " + response.getBody());
        }
    }


    private String createFineTuningJob(String fileId, String modelName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("training_file", fileId);
        requestBody.put("model", modelName);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(API_KEY);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(OPENAI_URL + "/v1/fine_tuning/jobs", requestEntity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            Map<String, Object> jsonResponse = parseJson(response.getBody());
            log.info("Fine-tuning job created: {}", response.getBody());
            if (jsonResponse != null) {
                return jsonResponse.get("id").toString();
            }else {
                return null;
            }
        }else {
            throw new RuntimeException("Failed to create fine-tuning job: " + response.getBody());
        }
    }


    private String checkFineTuningJobStatus(String jobId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(API_KEY);

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        while (true) {
            ResponseEntity<String> response = restTemplate.exchange(OPENAI_URL + "/v1/fine_tuning/jobs/" + jobId, HttpMethod.GET, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> jsonResponse = parseJson(response.getBody());
                if (jsonResponse == null) {
                    continue;
                }
                String status = (String) jsonResponse.get("status");
                if ("succeeded".equalsIgnoreCase(status)) {
                    return jsonResponse.get("model").toString();
                }else if ("failed".equalsIgnoreCase(status)) {
                    break;
                }
            }else {
                continue;
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }
        }
        return null;
    }


    private Map<String, Object> parseJson(String json) {
        return JSON.parseObject(json, Map.class);
    }

}
