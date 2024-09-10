package com.github.hambuger.memory.chat.memory.tools.websearch;

import com.google.common.collect.Lists;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.learn.LearnProceduralMemory;
import com.github.hambuger.memory.chat.memory.other.functionCall.aop.FunctionCallRegistry;
import com.github.hambuger.memory.chat.memory.tools.webpage.PageDetailGet;

import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;


/**
 * @author hamburger
 * @since 2024/7/23
 */
@Slf4j
@Component
public class BaiduSearch {

    @Resource
    private PageDetailGet pageDetailGet;

    @Resource
    private LearnProceduralMemory learnProceduralMemory;

    private static final String SEARCH_URL = "http://www.baidu.com/s?pn=0&wd=%s";


    @Data
    public static class BaiduQuery {

        @JsonPropertyDescription("Search for text")
        @JsonProperty(required = true)
        private String queryText;

    }


    @FunctionCallRegistry(functionDesc = "Go to Baidu to search for relevant information", scene = {ChatSceneEnum.LEARN_FUNCTION})
    public String getSearchResultFromBaidu(BaiduQuery query) {
        try {
            String basePath = Paths.get("memory-chat-memory/src/main/java/com/github/hambuger/memory/chat/memory/tools/pythons").toAbsolutePath() + "/websearch";
            List<String> paths = Lists.newArrayList(basePath + "/search.py", basePath + "/base_search.py", basePath + "/exceptions.py", basePath + "/utils.py");
            String result = learnProceduralMemory.invokePythonFunction(paths, "get_baidu", new HashMap<>() {{
                put("word", query.queryText);
            }});
            Map<String, String> urlMap = JSON.parseObject(result, Map.class);
            return pageDetailGet.fetchUrlListContent(query.getQueryText(), urlMap.keySet().stream().toList());
        } catch (Exception ex) {
            log.error("There was an error in the search", ex);
        }
        return "Search Results: Empty";
    }

    @FunctionCallRegistry(functionDesc = "Go to Baidu to search for relevant information", scene = {ChatSceneEnum.NORMAL_USER, ChatSceneEnum.NORMAL_GROUP, ChatSceneEnum.SCHEDULE, ChatSceneEnum.NEWS_SCHEDULE, ChatSceneEnum.TASK, ChatSceneEnum.LEARN_SKILL})
    public String getBaiduSearchResult(BaiduQuery query) {
        try {
            String basePath = Paths.get("memory-chat-memory/src/main/java/com/github/hambuger/memory/chat/memory/tools/pythons").toAbsolutePath() + "/websearch";
            List<String> paths = Lists.newArrayList(basePath + "/search.py", basePath + "/base_search.py", basePath + "/exceptions.py", basePath + "/utils.py");
            String result = learnProceduralMemory.invokePythonFunction(paths, "get_baidu", new HashMap<>() {{
                put("word", query.queryText);
            }});
            return result;
        } catch (Exception ex) {
            log.error("There was an error in the search", ex);
        }
        return "Search Results: Empty";
    }

}
