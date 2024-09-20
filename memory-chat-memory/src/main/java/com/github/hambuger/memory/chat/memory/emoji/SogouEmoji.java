package com.github.hambuger.memory.chat.memory.emoji;

import com.github.hambuger.memory.chat.memory.other.util.MyHttpUtils;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;


/**
 * @author hamburger
 * @since 2024/8/8
 */
@Slf4j
@Component
public class SogouEmoji {

    @Value("${emoji.directory}")
    private String directory;

    private final static String emojiWebAddress = "https://pic.sogou.com/pic/emo/searchList.jsp?keyword=%s&spver=&rcer=&routeName=emosearch&tag=0";

    {
        EmojiManager.registerChannel("sougou", this::searchEmoticonPhoto);
    }

    public String searchEmoticonPhoto(EmoticonPictureQuery query) {
        try {
            log.info("emoji query:{}", query.getEmoticonPictureQueryWord());
            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
            headers.put("Accept-Encoding", "gzip, deflate, br, zstd");
            headers.put("Accept-Language", "zh-CN,zh;q=0.9");
            headers.put("Connection", "keep-alive");
            headers.put("Host", "pic.sogou.com");
            headers.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");
            String response = MyHttpUtils.get(String.format(emojiWebAddress, URLEncoder.encode(query.getEmoticonPictureQueryWord(), StandardCharsets.UTF_8)), headers, null);
            Random random = new Random();
            int randomNumber = random.nextInt(5);
            String regex;
            regex = "\"thumbSrc\":\"(https:[^\"]+)\",\"idx\":" + randomNumber;
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

    public static String getFileNameFromUrl(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }

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
            }
        }
        return "png";
    }

    // Determine whether it is in PNG format
    private static boolean isPNG(byte[] header) {
        return (header[0] == (byte) 0x89 && header[1] == (byte) 0x50 && header[2] == (byte) 0x4E && header[3] == (byte) 0x47);
    }

    // Determine whether it is in JPEG format
    private static boolean isJPEG(byte[] header) {
        return (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8);
    }

    // Determine whether it is in GIF format
    private static boolean isGIF(byte[] header) {
        return (header[0] == (byte) 0x47 && header[1] == (byte) 0x49 && header[2] == (byte) 0x46);
    }

    // Download and save images
    public static void downloadImageFromUrl(String imageUrl, String destinationFile) throws IOException {
        URL url = new URL(imageUrl);
        try (InputStream in = url.openStream()) {
            Files.copy(in, Paths.get(destinationFile));
        }
    }



    /**
     * Download pictures to local
     */
    private static String downloadImage(String imageUrl, String destinationFilePath) throws Exception {
        HttpResponse<InputStream> response = Unirest.get(imageUrl).asBinary();
        try (InputStream in = response.getBody()) {

            // Get the content type and set the file extension based on this information
            List<String> contentTypes = response.getHeaders().get("Content-Type");
            if (contentTypes != null && contentTypes.size() > 0) {
                destinationFilePath = destinationFilePath + UUID.randomUUID() +contentTypes.get(0).replace("image/", ".");
            }else {
                destinationFilePath = destinationFilePath  + UUID.randomUUID() + ".png";
            }

            File file = new File(destinationFilePath);
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

            try (FileOutputStream out = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            return destinationFilePath;

        } catch (Exception e) {
            log.error("downloadImage error", e);
            return null;
        }
    }

}
