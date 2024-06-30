package com.github.hambuger.memory.chat.memory.util;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.google.common.collect.Lists;
import com.mashape.unirest.http.exceptions.UnirestException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ImageUploadUtils {

    @Resource
    private UploadGitHubImgBed uploadGitHubImgBed;

    private static final ThreadPoolExecutor UPLOAD_POOL = new ThreadPoolExecutor(10, 20, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000), new CustomizableThreadFactory("upload-pool"), new ThreadPoolExecutor.CallerRunsPolicy());


    public String uploadImg(String originalFilename) throws UnirestException {
        if (originalFilename == null) {
            log.error("图片不存在");
        }
        String targetURL = uploadGitHubImgBed.createUploadFileUrl(originalFilename);
        Map<String, Object> uploadBodyMap = uploadGitHubImgBed.getUploadBodyMap(FileUtil.getFileBase64Data(originalFilename, false));
        Map<String, String> header = uploadGitHubImgBed.getUploadHeader();
        return getDownloadUrlWithRetry(targetURL, uploadBodyMap, header);
    }

    private String getDownloadUrlWithRetry(String targetURL, Map<String, Object> uploadBodyMap, Map<String, String> header) throws UnirestException {
        int tryCount = 6;
        for(int i= 0; i< tryCount; i++){
            String JSONResult = MyHttpUtils.put(targetURL, uploadBodyMap, header);
            JSONObject jsonObj = JSONUtil.parseObj(JSONResult);
            //请求失败
            if (jsonObj == null || jsonObj.getObj("commit") == null) {
                String regex = "expected\\s+([a-fA-F0-9]{40})";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(jsonObj.get("message").toString());
                if (matcher.find()) {
                    // 获取匹配的哈希值
                    String expectedHash = matcher.group(1);
                    header.put("sha", expectedHash);
                }
                continue;
            }
            JSONObject content = JSONUtil.parseObj(jsonObj.getObj("content"));
            String downloadUrl = (String) content.getObj("download_url");
            return downloadUrl;
        }
        log.error("图片上传失败");
        return null;
    }

    public List<String> uploadImageList(List<String> imagePathList) {
        List<String> result = new ArrayList<>();
        if (CollectionUtils.isEmpty(imagePathList)) {
            return result;
        }
        if (imagePathList.size() == 1) {
            try {
                return Lists.newArrayList(uploadImg(imagePathList.get(0)));
            } catch (Exception e) {
                log.error("uploadImageList error", e);
                return result;
            }
        } else {
            CountDownLatch latch = new CountDownLatch(imagePathList.size());
            ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
            for (int i = 0; i < imagePathList.size(); i++) {
                String image = imagePathList.get(i);
                UPLOAD_POOL.execute(() -> {
                    try {
                        map.put(image, uploadImg(image));
                    } catch (Exception e) {
                        log.error("uploadImageList error", e);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            try {
                latch.await(60, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("uploadImageList error", e);
            }
            for (String image : imagePathList) {
                Optional.ofNullable(map.get(image)).ifPresent(result::add);
            }
            return result;
        }
    }

}


