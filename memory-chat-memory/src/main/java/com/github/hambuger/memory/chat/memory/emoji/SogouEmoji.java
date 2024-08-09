package com.github.hambuger.memory.chat.memory.emoji;

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
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;


/**
 * @author hanjiabao
 * @since 2024/8/8
 */
@Slf4j
@Component
public class SogouEmoji {

    @Value("${emoji.directory}")
    private String directory;

    private final static String emojiWebAddress = "https://pic.sogou.com/pic/emo/searchList.jsp?keyword=%s&spver=&rcer=&routeName=emosearch&tag=0";


    public String searchEmoji(String keyword) {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
            headers.put("Accept-Encoding", "gzip, deflate, br, zstd");
            headers.put("Accept-Language", "zh-CN,zh;q=0.9");
            headers.put("Connection", "keep-alive");
            headers.put("Host", "pic.sogou.com");
            headers.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");
            String response = MyHttpUtils.get(String.format(emojiWebAddress, keyword), headers, null);
            Random random = new Random();
            int randomNumber = random.nextInt(21) - 10;
            String regex;
            if (randomNumber > 0) {
                regex = "\"thumbSrc\":\"(https:[^\"]+)\",\"idx\":" + randomNumber;
            }else {
                regex = "\"emoGroupList\":\\[\\[\\{\"groupName\":\"[^\"]+\",\"groupId\":[0-9]+,\"picUrl\":\"(https:[^\"]+)\",\"pic";
            }
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(response);
            if (matcher.find()) {
                String rawUrl = matcher.group(1);
                String decodedUrl = rawUrl.replace("\\u002F", "/");
                String downUrl = directory + keyword;
                return downloadImage(decodedUrl, downUrl);
            }else {
                return null;
            }
        } catch (Exception e) {
            log.error("searchEmoji error", e);
        }
        return null;
    }


    /**
     * 下载图片到本地
     */
    private static String downloadImage(String imageUrl, String destinationFilePath) throws Exception {
        HttpResponse<InputStream> response = Unirest.get(imageUrl).asBinary();
        try (InputStream in = response.getBody()) {

            // 获取内容类型并根据此信息设置文件扩展名
            List<String> contentTypes = response.getHeaders().get("Content-Type");
            if (contentTypes != null && contentTypes.size() > 0) {
                destinationFilePath = destinationFilePath + contentTypes.get(0).replace("image/", ".");
            }else {
                destinationFilePath = destinationFilePath + ".png";
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
