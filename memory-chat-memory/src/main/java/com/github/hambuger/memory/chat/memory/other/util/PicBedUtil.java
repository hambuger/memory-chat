package com.github.hambuger.memory.chat.memory.other.util;


import com.alibaba.fastjson.JSONObject;

import net.coobird.thumbnailator.Thumbnails;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
public class PicBedUtil {

    @Value("${image.upload.telegraph_url}")
    private String uploadUrl;

    @Value("${temp.path}")
    private String tempPath;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;


    private static final RestTemplate restTemplate = new RestTemplate();

    public String uploadImage(String file) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(zipImage(file)));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(uploadUrl + "/upload", HttpMethod.POST, requestEntity, String.class);
        return uploadUrl + ((JSONObject) (JSONObject.parseArray(response.getBody()).get(0))).getString("src");
    }


    public List<String> uploadImages(List<String> files) {
        List<String> result = new ArrayList<>();
        Map<String, String> imageUrlMap = new ConcurrentHashMap<>();
        files.parallelStream().forEach(file -> {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(zipImage(file)));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(uploadUrl + "/upload", HttpMethod.POST, requestEntity, String.class);
            imageUrlMap.put(file, uploadUrl + ((JSONObject) (JSONObject.parseArray(response.getBody()).get(0))).getString("src"));
        });
        for (String image : files) {
            Optional.ofNullable(imageUrlMap.get(image)).ifPresent(result::add);
        }
        return result;
    }


    public File zipImage(String imagePath) {
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            return null;
        }
        long fileSize = imageFile.length();

        if (fileSize > MAX_FILE_SIZE) {
            try {
                return compressImage(imageFile);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
        return imageFile;
    }


    private File compressImage(File inputFile) throws IOException {
        String newFileName = tempPath + File.separator + "compressed_" + inputFile.getName();
        File outputFile = new File(newFileName);
        double quality = 1.0;
        while (outputFile.length() == 0 || outputFile.length() > MAX_FILE_SIZE) {
            quality -= 0.1;
            Thumbnails.of(inputFile).scale(1.0).outputQuality(quality).toFile(outputFile);
            if (quality <= 0.1) {
                break;
            }
        }
        return outputFile;
    }
}

