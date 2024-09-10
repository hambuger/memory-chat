package com.github.hambuger.memory.chat.memory.other.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class UploadGitHubImgBed {
    /**
     * Github private token
     */
    @Value("${github.accessToken}")
    public String ACCESS_TOKEN;

    /**
     * github username
     */
    @Value("${github.owner}")
    public String OWNER;

    /**
     * Upload specified warehouse
     */
    @Value("${github.repo}")
    public String REPO;


    /**
     * Specify the path to store the image when uploading
     */
    @Value("${github.path}")
    public String PATH;


    /**
     * Used to submit description
     */
    @Value("${github.addMessage}")
    public String ADD_MESSAGE;

    @Value("${github.delMessage}")
    public String DEL_MESSAGE;

    @Value("${github.apiCreatePost}")
    public String API_CREATE_POST;

    @Value("${github.branch}")
    public String BRANCH;

    @Value("${github.email}")
    public String EMAIL;

    @Value("${github.accept}")
    public String ACCEPT;

    @Value("${github.contentType}")
    public String CONTENTTYPE;


    public static String getFileExtension(String fileName) {
        int lastIndexOfDot = fileName.lastIndexOf('.');
        if (lastIndexOfDot == -1) {
            return "";
        }
        return fileName.substring(lastIndexOfDot);
    }

    /**
     *
     * @param originalFilename
     * @return
     */
    public String createUploadFileUrl(String originalFilename) {

        String suffix = getFileExtension(originalFilename);

        String fileName = System.currentTimeMillis() + StringUtils.replace(UUID.randomUUID().toString(), "-", "") + suffix;

        String url = String.format(API_CREATE_POST, OWNER, REPO, PATH + "/" + fileName);
        return url;
    }

    /**
     * Get the request body map collection of the created file：access_token、message、content
     */
    public Map<String, Object> getUploadBodyMap(String base64Data) {
        HashMap<String, Object> bodyMap = new HashMap<>(2);
        bodyMap.put("message", ADD_MESSAGE);
        bodyMap.put("content", base64Data);
        bodyMap.put("branch", BRANCH);
        bodyMap.put("committer", new HashMap<>() {{
            put("name", OWNER);
            put("email", EMAIL);
        }});
        return bodyMap;
    }

    public Map<String, String> getUploadHeader() {
        Map<String, String> header = new HashMap<>();
        header.put("Authorization", ACCESS_TOKEN);
        header.put("Accept", ACCEPT);
        header.put("Content-Type", CONTENTTYPE);
        return header;
    }
}


