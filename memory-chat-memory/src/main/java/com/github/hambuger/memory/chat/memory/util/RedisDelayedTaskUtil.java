package com.github.hambuger.memory.chat.memory.util;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.hambuger.memory.chat.memory.chat.SpringAiChat;
import com.github.hambuger.memory.chat.memory.chat.dto.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.functionCall.aop.FunctionCallRegistry;
import com.google.common.collect.Lists;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RedisDelayedTaskUtil {

    @Resource
    private SpringAiChat springAiChat;

    @Autowired
    private RedisTemplate<String, Object> commonRedisTemplate;

    private static final String DELAYED_TASK_KEY = "delayedTasks";

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TaskInfo {

        @JsonPropertyDescription("延迟任务的详细内容描述")
        @JsonProperty(required = true)
        private String taskMessage;

        @JsonPropertyDescription("延迟时间")
        @JsonProperty(required = true)
        private long delayTime;

        @JsonPropertyDescription("延迟时间单位")
        @JsonProperty(required = true)
        private TimeUnit timeUnit;


    }


    // 添加延迟任务
    @FunctionCallRegistry(functionDesc = "添加一个任务，以便在未来时间处理", scene = {ChatSceneEnum.NORMAL_USER, ChatSceneEnum.NORMAL_GROUP, ChatSceneEnum.TASK})
    public boolean addTask(TaskInfo taskInfo) {
        // 将自定义时间单位转换为秒
        long delayInSeconds = taskInfo.getTimeUnit().toSeconds(taskInfo.getDelayTime());
        long executionTime = Instant.now().getEpochSecond() + delayInSeconds;
        // 序列化 TaskInfo 对象为 JSON 字符串
        String jsonString = JSON.toJSONString(taskInfo);

        // 将序列化后的 JSON 字符串存储到 Redis 中
        commonRedisTemplate.opsForZSet().add(DELAYED_TASK_KEY, jsonString, executionTime);
        return true;

    }

    // 执行到期任务
    public void executeTasks() {
        while (true) {
            long now = Instant.now().getEpochSecond();

            Set<Object> tasks = commonRedisTemplate.opsForZSet().rangeByScore(DELAYED_TASK_KEY, 0, now);

            if (tasks == null || tasks.isEmpty()) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    log.warn("delayedTasks error", e);
                }
                continue;
            }

            for (Object taskObj : tasks) {
                log.info("Executing task: " + taskObj + " at " + Instant.now().getEpochSecond());
                TaskInfo taskInfo = JSON.parseObject(taskObj.toString(), TaskInfo.class);
                List<OpenAiApi.ChatCompletionMessage> messages = Lists.newArrayList(new OpenAiApi.ChatCompletionMessage(getDelayedTaskPrompt(taskInfo), OpenAiApi.ChatCompletionMessage.Role.SYSTEM));
                springAiChat.generateMsgWithMsgListAndFunctions(messages, false, ChatSceneEnum.TASK);
                commonRedisTemplate.opsForZSet().remove(DELAYED_TASK_KEY, taskObj);
            }
        }
    }

    private String getDelayedTaskPrompt(TaskInfo taskInfo) {
        Map<TimeUnit, String> timeUnitToChineseMap = new HashMap<>();
        // 填充 Map
        timeUnitToChineseMap.put(TimeUnit.SECONDS, "秒");
        timeUnitToChineseMap.put(TimeUnit.MINUTES, "分钟");
        timeUnitToChineseMap.put(TimeUnit.HOURS, "小时");
        timeUnitToChineseMap.put(TimeUnit.DAYS, "天");
            return String.format("""
你是Andrew,一个30岁的中国年轻人。
你在%s之前给自己写了一个便签，以便提醒自己要做的事情。
以下是便签的内容:
%s

按照便签的指引完成要做的事。
现在时间点是：%s
 """, taskInfo.getDelayTime() + timeUnitToChineseMap.get(taskInfo.getTimeUnit()), taskInfo.getTaskMessage(), DateUtil.now());

    }

    // 在项目启动时启动延迟任务监听
    @PostConstruct
    public void init() {
        new Thread(this::executeTasks).start();
    }
}
