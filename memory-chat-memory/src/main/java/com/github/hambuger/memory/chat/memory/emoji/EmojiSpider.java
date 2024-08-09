package com.github.hambuger.memory.chat.memory.emoji;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.hambuger.memory.chat.memory.other.util.MyHttpUtils;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;


/**
 * @author hanjiabao
 * @since 2024/7/15
 */

@Slf4j
@Component
public class EmojiSpider {


    @Value("${emoji.directory}")
    private String directory;

    // 网址
    private final static String emojiWebAddress = "https://www.dbbqb.com/api/search/json?start=0&w=/%s";


    public String searchEmoji(String keyword) {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "application/json");
            headers.put("Accept-Encoding", "gzip, deflate, br, zstd");
            headers.put("Accept-Language", "zh-CN,zh;q=0.9");
            headers.put("Connection", "keep-alive");
            headers.put("Content-Type", "application/json");
            headers.put("Host", "www.dbbqb.com");
            headers.put("Referer", String.format("https://www.dbbqb.com/s?w=/%s", keyword));
            headers.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");
            headers.put("Web-Agent", "web");


            String response = MyHttpUtils.get(String.format(emojiWebAddress, keyword), headers, null);
            JSONArray array = JSONArray.parseArray(response);
            if (!array.isEmpty()) {
                JSONObject object = array.getJSONObject(0);
                String url = "https://image.dbbqb.com/" + object.getString("path");
                String downUrl = directory + keyword;
                return downloadImage(url, downUrl);
            }
        } catch (Exception e) {
            log.error("searchEmoji error", e);
        }
        return null;
    }


    /**
     * 下载图片到本地
     */
    private String downloadImage(String imageUrl, String destinationFilePath) throws Exception {
        HttpResponse<InputStream> response = Unirest.get(imageUrl).asBinary();
        try (InputStream in = response.getBody()) {

            // 获取内容类型并根据此信息设置文件扩展名
            List<String> contentTypes = response.getHeaders().get("Content-Type");
            if (contentTypes != null && contentTypes.size() > 0) {
                destinationFilePath = destinationFilePath + contentTypes.get(0).replace("image/", ".");
            }

            // 确保目录存在
            File file = new File(destinationFilePath);
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();  // 创建所有必要的父目录
            }

            try (FileOutputStream out = new FileOutputStream(file)) {
                // 写入文件
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            return destinationFilePath;

        } catch (Exception e) {
            log.error("downloadImage error", e);
            return null;  // 返回null表示下载失败
        }
    }
}

