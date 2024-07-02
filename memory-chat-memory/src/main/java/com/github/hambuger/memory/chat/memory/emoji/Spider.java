package com.github.hambuger.memory.chat.memory.emoji;

/**
 * @author hanjiabao
 * @since 2024/7/2
 */

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;


public class Spider {

    public static void main(String[] args) {
        String baseUrl = "http://fabiaoqing.com";
        String searchUrl = baseUrl + "/search/bqb/keyword/药水哥/type/bq/page/1.html";

        try {
            // 获取页面内容
            Document doc = Jsoup.connect(searchUrl).userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36").get();

            // 创建保存图片的目录
            String directory = "./images/";
            Files.createDirectories(Paths.get(directory));

            // 解析图片信息
            Elements imgList = doc.select("img.ui.image.bqppsearch.lazy");
            for (Element img : imgList) {
                String imgUrl = img.attr("data-original");
                String imgTitle = img.attr("title");

                System.out.println(imgUrl + " " + imgTitle);

                try {
                    // 构造图片保存路径
                    String extension = imgUrl.substring(imgUrl.lastIndexOf("."));
                    String filePath = directory + imgTitle + extension;

                    // 下载图片
                    downloadImage(imgUrl, filePath);

                    System.out.println("保存成功: " + imgTitle);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // 下载图片方法
    private static void downloadImage(String imageUrl, String destinationFilePath) throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");
        headers.put("Referer", "https://fabiaoqing.com/");
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

