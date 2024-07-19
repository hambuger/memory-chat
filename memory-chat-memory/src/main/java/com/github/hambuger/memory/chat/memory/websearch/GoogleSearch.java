package com.github.hambuger.memory.chat.memory.websearch;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;


/**
 * @author hanjiabao
 * @since 2024/7/19
 */
public class GoogleSearch {

    public static void main(String[] args) throws IOException {

        String googleUrl = "https://www.google.com/search?q=希音&gl=cn&hl=zh-cn";

        // Connect to the Google search page
        Document doc = Jsoup.connect(googleUrl).userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/101.0.4951.54 Safari/537.36").get();
        Elements results = doc.select("div.yuRUbf");
        for (Element result : results) {
            String link = result.select("a[jsname=UWckNb]").attr("href");
            System.out.println(link);
        }
    }

}
