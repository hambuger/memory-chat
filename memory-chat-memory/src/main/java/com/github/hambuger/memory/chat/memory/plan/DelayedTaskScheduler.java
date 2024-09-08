package com.github.hambuger.memory.chat.memory.plan;

import com.alibaba.fastjson.JSON;
import com.github.hambuger.memory.chat.memory.chat.ChatCompletionsApi;
import com.github.hambuger.memory.chat.memory.chat.model.ChatMember;
import com.github.hambuger.memory.chat.memory.tools.websearch.HotNews;
import com.github.hambuger.memory.chat.memory.other.util.RedisUtil;

import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DelayedTaskScheduler {

    @Resource
    private ChatCompletionsApi chatCompletionsApi;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private HotNews hotNews;


    @Scheduled(fixedRate = 1000 * 60 * 30)
    public void processTasks() {
        List<Object> allMembers = redisUtil.getAllMembers();
        if (CollectionUtils.isEmpty(allMembers)) {
            return;
        }
        allMembers.parallelStream().forEach(this::executeSchedulerTask);
    }

    private void executeSchedulerTask(Object member) {
        ChatMember chatMember = JSON.parseObject(member.toString(), ChatMember.class);
        String news = getRecentNews();
        if (StringUtils.isBlank(news)) {
            return;
        }
        chatCompletionsApi.executeSchedulerTask(chatMember, news);
    }

    private String getRecentNews() {
        return hotNews.getRecentNews();
    }


}

