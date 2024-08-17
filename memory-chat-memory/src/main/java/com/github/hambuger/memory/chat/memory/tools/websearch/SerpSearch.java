package com.github.hambuger.memory.chat.memory.tools.websearch;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.hambuger.memory.chat.memory.other.util.MyHttpUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class SerpSearch {

    @Value("${serp.apiKey}")
    public String apikey;

    @Value("${serp.baseUrl}")
    public String baseUrl;

    @Data
    public static class SerpQuery {

        @JsonPropertyDescription("搜索关键词")
        @JsonProperty(required = true)
        private String queryWord;
    }


//    @FunctionCallRegistry(functionDesc = "去谷歌搜索相关信息")
    public String getSerpSearchResult(SerpQuery query) {
        try {
            Map<String, Object> queryParam = new HashMap<>();
            queryParam.put("q", URLEncoder.encode(query.queryWord, StandardCharsets.UTF_8));
            queryParam.put("api_key", apikey);
            queryParam.put("hl", "zh-cn");
            queryParam.put("gl", "cn");
            queryParam.put("safe", "off");
            queryParam.put("device", "desktop");
//            queryParam.put("tbm", "nws");
            String json = MyHttpUtils.get(baseUrl, new HashMap<>(), queryParam);
            JSONObject results = JSON.parseObject(json);
            StringBuilder stringBuilder = new StringBuilder();
            JSONArray jsonArray = results.getJSONArray("organic_results");
            if (CollectionUtils.isEmpty(jsonArray)) {
                return null;
            }
            for (int i = 1; i <= jsonArray.size(); i++) {
                JSONObject result = (JSONObject) jsonArray.get(i - 1);
                String title = result.get("title").toString();
                String snippet = result.get("snippet").toString();
                stringBuilder.append(i).append(".").append("《").append(title).append("》").append("\n").append(snippet).append("。\n\n");
            }
            return stringBuilder.toString();
        } catch (Exception e) {
            log.error("search error", e);
        }
        return null;
    }

}
