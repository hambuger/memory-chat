package com.github.hambuger.memory.chat.memory.util;

import com.github.hambuger.memory.chat.memory.chat.ChatCompletionsApi;
import com.github.hambuger.memory.chat.memory.chat.dto.ChatMember;
import com.github.hambuger.memory.chat.memory.websearch.HotNews;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

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
        Set<Object> allMembers = redisUtil.getAllMembers();
        if (CollectionUtils.isEmpty(allMembers)) {
            return;
        }
        allMembers.parallelStream().forEach(this::executeSchedulerTask);
    }

    private void executeSchedulerTask(Object member) {
        ChatMember chatMember = (ChatMember) member;
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

