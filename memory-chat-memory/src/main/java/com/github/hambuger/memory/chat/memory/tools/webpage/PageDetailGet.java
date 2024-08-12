package com.github.hambuger.memory.chat.memory.tools.webpage;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.other.functionCall.aop.FunctionCallRegistry;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;


/**
 * @author hanjiabao
 * @since 2024/8/12
 */
@Slf4j
@Component
public class PageDetailGet {

    private static final ThreadPoolExecutor FETCH_URL_POOL = new ThreadPoolExecutor(20, 20, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(2000));

    private Browser browser;


    @Data
    @AllArgsConstructor
    public static class WebPageUrl {

        @JsonPropertyDescription("网页url")
        @JsonProperty(required = true)
        private String webPageUrl;
    }


    @PostConstruct
    public void init() {
        // 初始化 Playwright 和 Browser 实例
        Playwright playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }


    public String fetchUrlListContent(List<String> urlList) {
        try {
            if (CollectionUtils.isEmpty(urlList)) {
                return null;
            }
            CountDownLatch latch = new CountDownLatch(urlList.size());
            Map<String, String> urlAndContent = new HashMap<>();
            for (String url : urlList) {
                FETCH_URL_POOL.execute(() -> {
                    try {
                        String content = getWebPageDetail(new WebPageUrl(url));
                        if (StringUtils.isNotBlank(content) && content.length() > 10) {
                            urlAndContent.put(url, content);
                        }
                    } catch (Exception e) {
                        log.error("fetchUrlListContent error", e);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await(30, TimeUnit.SECONDS);
            int index = 1;
            StringBuilder stringBuilder = new StringBuilder();
            for (String url : urlList) {
                if (urlAndContent.get(url) == null) {
                    continue;
                }
                stringBuilder.append(index).append(". ").append(urlAndContent.get(url)).append("\n");
                index++;
                if (index > 6) {
                    break;
                }
            }
            return stringBuilder.toString();
        } catch (Exception e) {
            return null;
        }
    }


    @FunctionCallRegistry(functionDesc = "获取网页内容", scene = {ChatSceneEnum.NORMAL_USER, ChatSceneEnum.NORMAL_GROUP, ChatSceneEnum.NEWS_SCHEDULE, ChatSceneEnum.TASK})
    public String getWebPageDetail(WebPageUrl url) {
        try {
            Page page = browser.newPage();
            page.navigate(url.getWebPageUrl());
            Document parse = Jsoup.parse(page.content());
            page.close();
            return parse.text();
        } catch (Exception e) {
            return fetchUrlContent(url.getWebPageUrl());
        }
    }


    public static String fetchUrlContent(String urlString) {
        try {
            Document doc = Jsoup.connect(urlString).get();
            return doc.text();
        } catch (Exception e) {
            log.error("Failed to fetch " + urlString + ": " + e.getMessage());
            return null;
        }
    }

}
