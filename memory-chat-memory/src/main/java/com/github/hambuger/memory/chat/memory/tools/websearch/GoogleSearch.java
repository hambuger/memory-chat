package com.github.hambuger.memory.chat.memory.tools.websearch;

import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.other.functionCall.aop.FunctionCallRegistry;
import com.github.hambuger.memory.chat.memory.tools.webpage.PageDetailGet;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;


/**
 * @author hamburger
 * @since 2024/7/19
 */
@Slf4j
@Component
public class GoogleSearch {

    @Resource
    private PageDetailGet pageDetailGet;

    private final static String searchUrl = "https://www.google.com/search?q=%s";


    @FunctionCallRegistry(functionDesc = "Go Google and search for relevant information", scene = {ChatSceneEnum.NORMAL_USER, ChatSceneEnum.NORMAL_GROUP, ChatSceneEnum.TASK})
    public String getGoogleSearchResult(SerpSearch.SerpQuery query) {
        try {
            String googleUrl = String.format(searchUrl, query.getQueryWord().replace(" ", "%20"));
            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
            headers.put("Accept-Language", "zh-CN,zh;q=0.9");
            headers.put("Priority", "u=0, i");
            headers.put("Sec-Ch-Ua", "\"Not/A)Brand\";v=\"8\", \"Chromium\";v=\"126\", \"Google Chrome\";v=\"126\"");
            headers.put("Sec-Ch-Ua-Arch", "\"x86\"");
            headers.put("Sec-Ch-Ua-Bitness", "\"64\"");
            headers.put("Sec-Ch-Ua-Full-Version", "\"126.0.6478.127\"");
            headers.put("Sec-Ch-Ua-Full-Version-List", "\"Not/A)Brand\";v=\"8.0.0.0\", \"Chromium\";v=\"126.0.6478.127\", \"Google Chrome\";v=\"126.0.6478.127\"");
            headers.put("Sec-Ch-Ua-Mobile", "?0");
            headers.put("Sec-Ch-Ua-Model", "");
            headers.put("Sec-Ch-Ua-Platform", "\"macOS\"");
            headers.put("Sec-Ch-Ua-Platform-Version", "\"14.4.1\"");
            headers.put("Sec-Ch-Ua-Wow64", "?0");
            headers.put("Sec-Fetch-Dest", "document");
            headers.put("Sec-Fetch-Mode", "navigate");
            headers.put("Sec-Fetch-Site", "none");
            headers.put("Sec-Fetch-User", "?1");
            headers.put("Upgrade-Insecure-Requests", "1");
            headers.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");
            headers.put("X-Client-Data", "CIy2yQEIprbJAQipncoBCJ78ygEIlaHLAQid/swBCPKYzQEIhqDNAQjok84BCLKWzgEI2pvOAQjGnc4BCK+ezgEIsp/OAQiYos4BCKaizgEI4afOARjX680BGKCdzgE=");
            Document doc =
                    Jsoup.connect(googleUrl).headers(headers).userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537" + ".36").get();
            Elements results = doc.select("div.yuRUbf");
            List<String> urlList = new ArrayList<>();
            for (Element result : results) {
                String link = result.select("a[jsname=UWckNb]").attr("href");
                if (StringUtils.isNotBlank(link)) {
                    urlList.add(link);
                }
            }
            return pageDetailGet.fetchUrlListContent(query.getQueryWord(), urlList);
        } catch (Exception e) {
            log.error("getSerpSearchResult error", e);
        }
        return null;
    }
}
