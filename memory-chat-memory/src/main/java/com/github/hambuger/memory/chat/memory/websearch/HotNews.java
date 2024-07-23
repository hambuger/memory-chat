package com.github.hambuger.memory.chat.memory.websearch;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
public class HotNews {

    public String getRecentNews() {
        String weiboUrl = "https://weibo.com/ajax/side/hotSearch";
        String weiboJson = HttpUtil.get(weiboUrl);
        List<Map<String, String>> realtimeList = (List<Map<String, String>>) JSONUtil.getByPath(JSONUtil.parse(weiboJson), "data.realtime");
        int index = 1;
        StringBuilder newsBuilder = new StringBuilder();
        for (Map<String, String> news : realtimeList) {
            if (index > 10) {
                break;
            }
            newsBuilder.append(index).append(". ");
            newsBuilder.append("热点标题：" + news.get("word")).append("; ");
            if (StringUtils.isNotBlank(news.get("category"))) {
                newsBuilder.append("热点分类：" + news.get("category"));
            }
            newsBuilder.append("\n");
            index++;
        }
        return newsBuilder.toString();
    }

}
