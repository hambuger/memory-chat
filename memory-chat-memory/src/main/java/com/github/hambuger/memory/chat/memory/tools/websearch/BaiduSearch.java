package com.github.hambuger.memory.chat.memory.tools.websearch;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.other.functionCall.aop.FunctionCallRegistry;
import com.github.hambuger.memory.chat.memory.tools.webpage.PageDetailGet;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;


/**
 * @author hanjiabao
 * @since 2024/7/23
 */
@Slf4j
@Component
public class BaiduSearch {

    @Resource
    private PageDetailGet pageDetailGet;

    private static final String SEARCH_URL = "http://www.baidu.com/s?pn=0&wd=%s";


    @Data
    public static class BaiduQuery {

        @JsonPropertyDescription("搜索文本")
        @JsonProperty(required = true)
        private String queryText;

    }


    @FunctionCallRegistry(functionDesc = "去百度搜索相关信息", scene = {ChatSceneEnum.NORMAL_USER, ChatSceneEnum.NORMAL_GROUP, ChatSceneEnum.NEWS_SCHEDULE, ChatSceneEnum.TASK, ChatSceneEnum.LEARN_SKILL, ChatSceneEnum.LEARN_FUNCTION})
    public String getBaiduSearchResult(BaiduQuery query) {
        try {
            Document document = Jsoup.connect(String.format(SEARCH_URL, query.getQueryText())).get();
            // 获取所有包含mu属性的div元素
            Elements divElements = document.select("div[srcid][mu]");

            // 创建一个列表来存储mu链接
            List<String> muLinks = new ArrayList<>();

            // 遍历每个div元素并提取mu属性的值
            for (Element div : divElements) {
                String muLink = div.attr("mu");
                muLinks.add(muLink);
            }
            return pageDetailGet.fetchUrlListContent(muLinks);

        } catch (Exception ex) {
            log.error("搜索出错", ex);
        }
        return "搜索结果：空";
    }

}
