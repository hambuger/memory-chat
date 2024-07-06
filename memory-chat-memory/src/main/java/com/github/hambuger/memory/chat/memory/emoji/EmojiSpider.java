package com.github.hambuger.memory.chat.memory.emoji;

/**
 * @author hanjiabao
 * @since 2024/7/2
 */

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import org.apache.commons.collections4.CollectionUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
public class EmojiSpider {

    @Value("${emoji.baseUrlPrefix}")
    private String baseUrl;

    @Value("${emoji.urlContent}")
    private String urlContent;

    @Value("${emoji.userAgent}")
    private String userAgent;

    @Value("${emoji.directory}")
    private String directory;


    public String searchEmoji(String word) {
        String searchUrl = baseUrl + String.format(urlContent, word);

        try {
            // 获取页面内容
            Document doc = Jsoup.connect(searchUrl).userAgent(userAgent).get();
            if (Files.notExists(Paths.get(directory))) {
                Files.createDirectories(Paths.get(directory));
            }

            // 解析图片信息
            Elements imgList = doc.select("img.ui.image.bqppsearch.lazy");
            if (CollectionUtils.isEmpty(imgList)) {
                return null;
            }
            Element img = imgList.get(0);
            String imgUrl = img.attr("data-original");
//            String imgTitle = img.attr("title");
            try {
                // 构造图片保存路径
                String extension = imgUrl.substring(imgUrl.lastIndexOf("."));
                String filePath = directory + UUID.randomUUID() + extension;

                // 下载图片
                downloadImage(imgUrl, filePath);
                return filePath;
            } catch (Exception e) {
                log.warn("searchEmoji error", e);
            }

        } catch (Exception e) {
            log.warn("searchEmoji error", e);
        }
        return null;
    }


    // 下载图片方法
    private void downloadImage(String imageUrl, String destinationFilePath) throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", userAgent);
        headers.put("Referer", baseUrl);
        HttpResponse<InputStream> response = Unirest.get(imageUrl).queryString(null).headers(headers).asBinary();
        try (InputStream in = response.getBody(); FileOutputStream out = new FileOutputStream(destinationFilePath)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
}

