package com.github.hambuger.memory.chat.memory.emoji;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.other.functionCall.aop.FunctionCallRegistry;
import com.github.hambuger.memory.chat.memory.other.util.MyHttpUtils;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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

    @Data
    public static class EmoticonPictureQuery {
        @JsonPropertyDescription("表情图片搜索文本")
        @JsonProperty(required = true)
        private String emoticonPictureQueryWord;

    }

    @FunctionCallRegistry(functionDesc = "搜索表情图片，返回图片url", scene = {ChatSceneEnum.NORMAL_GROUP, ChatSceneEnum.NORMAL_USER})
    public String searchEmoticonPhoto(EmoticonPictureQuery query) {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
            headers.put("Accept-Encoding", "gzip, deflate, br, zstd");
            headers.put("Accept-Language", "zh-CN,zh;q=0.9");
            headers.put("Connection", "keep-alive");
            headers.put("Host", "pic.sogou.com");
            headers.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");
            String response = MyHttpUtils.get(String.format(emojiWebAddress, URLEncoder.encode(query.getEmoticonPictureQueryWord(), StandardCharsets.UTF_8)), headers, null);
            Random random = new Random();
            int randomNumber = random.nextInt(11) - 5;
            String regex;
            if (randomNumber > 0) {
                regex = "\"thumbSrc\":\"(https:[^\"]+)\",\"idx\":" + randomNumber;
            } else {
                regex = "\"emoGroupList\":\\[\\[\\{\"groupName\":\"[^\"]+\",\"groupId\":[0-9]+,\"picUrl\":\"(https:[^\"]+)\",\"pic";
            }
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(response);
            if (matcher.find()) {
                String rawUrl = matcher.group(1);
                return rawUrl.replace("\\u002F", "/");
            }
        } catch (Exception e) {
            log.error("searchEmoji error", e);
        }
        return null;
    }

    public String searchEmoji(String keyword) {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
            headers.put("Accept-Encoding", "gzip, deflate, br, zstd");
            headers.put("Accept-Language", "zh-CN,zh;q=0.9");
            headers.put("Connection", "keep-alive");
            headers.put("Host", "pic.sogou.com");
            headers.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");
            String response = MyHttpUtils.get(String.format(emojiWebAddress, URLEncoder.encode(keyword, StandardCharsets.UTF_8)), headers, null);
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

    public String downloadImage(String imageUrl) {
        try {
            String fileName = getFileNameFromUrl(imageUrl);
            String format = detectImageFormat(imageUrl);
            String destinationFile = directory + fileName + "." + format;
            File file = new File(destinationFile);
            if (file.exists()) {
                return destinationFile;
            }
            downloadImageFromUrl(imageUrl, destinationFile);
            return destinationFile;
        } catch (IOException e) {
            log.error("downloadImage error", e);
        }
        return null;
    }

    // 从URL中提取文件名
    public static String getFileNameFromUrl(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }

    // 检测图片格式
    public static String detectImageFormat(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoInput(true);

        try (InputStream inputStream = connection.getInputStream()) {
            byte[] header = new byte[8];
            if (inputStream.read(header) != -1) {
                if (isPNG(header)) {
                    return "png";
                } else if (isJPEG(header)) {
                    return "jpg";
                } else if (isGIF(header)) {
                    return "gif";
                }
                // 扩展其他图片类型时可在此添加判断
            }
        }
        return "png";
    }

    // 判断是否为PNG格式
    private static boolean isPNG(byte[] header) {
        return (header[0] == (byte) 0x89 && header[1] == (byte) 0x50 && header[2] == (byte) 0x4E && header[3] == (byte) 0x47);
    }

    // 判断是否为JPEG格式
    private static boolean isJPEG(byte[] header) {
        return (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8);
    }

    // 判断是否为GIF格式
    private static boolean isGIF(byte[] header) {
        return (header[0] == (byte) 0x47 && header[1] == (byte) 0x49 && header[2] == (byte) 0x46);
    }

    // 下载并保存图片
    public static void downloadImageFromUrl(String imageUrl, String destinationFile) throws IOException {
        URL url = new URL(imageUrl);
        try (InputStream in = url.openStream()) {
            Files.copy(in, Paths.get(destinationFile));
        }
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
                destinationFilePath = destinationFilePath + UUID.randomUUID() +contentTypes.get(0).replace("image/", ".");
            }else {
                destinationFilePath = destinationFilePath  + UUID.randomUUID() + ".png";
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
