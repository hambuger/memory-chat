package com.github.hambuger.memory.chat.memory.plan;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.hambuger.memory.chat.memory.chat.SpringAiChat;
import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.other.functionCall.aop.FunctionCallRegistry;
import com.github.hambuger.memory.chat.memory.other.prompt.PromptFactory;
import com.github.hambuger.memory.chat.memory.other.util.UserInfoUtil;
import com.google.common.collect.Lists;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class DelayedTask {

    @Resource
    private SpringAiChat springAiChat;

    @Autowired
    private RedisTemplate<String, Object> commonRedisTemplate;

    @Resource
    private PromptFactory promptFactory;

    private static final String DELAYED_TASK_KEY = "delayedTasks";

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TaskInfo {

        @JsonPropertyDescription("Detailed description of delayed tasks")
        @JsonProperty(required = true)
        private String taskMessage;

        @JsonPropertyDescription("Delay time")
        @JsonProperty(required = true)
        private long delayTime;

        @JsonPropertyDescription("Delay time unit")
        @JsonProperty(required = true)
        private TimeUnit timeUnit;

        @JsonIgnore
        private String ownerName;


    }


    // Add a deferral task
    @FunctionCallRegistry(functionDesc = "Add a task to work on at a future time", scene = {ChatSceneEnum.NORMAL_USER, ChatSceneEnum.NORMAL_GROUP, ChatSceneEnum.TASK})
    public Boolean addTask(TaskInfo taskInfo) {
        // Convert custom time unit to seconds
        long delayInSeconds = taskInfo.getTimeUnit().toSeconds(taskInfo.getDelayTime());
        long executionTime = Instant.now().getEpochSecond() + delayInSeconds;
        // Serialize TaskInfo object to JSON string
        taskInfo.setOwnerName(UserInfoUtil.getUser());
        String jsonString = JSON.toJSONString(taskInfo);

        // Store the serialized JSON string in Redis
        commonRedisTemplate.opsForZSet().add(DELAYED_TASK_KEY, jsonString, executionTime);
        return true;

    }

    public String getAllTask(String owner) {
        StringBuilder builder = new StringBuilder();
        Set<ZSetOperations.TypedTuple<Object>> tuples = commonRedisTemplate.opsForZSet().rangeWithScores(DELAYED_TASK_KEY, 0, -1);
        if (tuples != null) {
            for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
                StringBuilder single = new StringBuilder();
                if (tuple.getScore() != null) {
                    Instant instant = Instant.ofEpochMilli(tuple.getScore().longValue() * 1000L);
                    ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
                    String formattedDateTime = zdt.format(formatter);
                    single.append(formattedDateTime).append(": ");
                }
                if (tuple.getValue() != null) {
                    TaskInfo taskInfo = JSON.parseObject(tuple.getValue().toString(), TaskInfo.class);
                    if (StringUtils.isNotBlank(owner) && StringUtils.equals(owner, taskInfo.getOwnerName())) {
                        single.append(taskInfo.getTaskMessage()).append("\n");
                        builder.append(single);
                    }
                }
            }
        }
        return builder.toString();
    }

    // Execute due tasks
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
        timeUnitToChineseMap.put(TimeUnit.SECONDS, "Seconds");
        timeUnitToChineseMap.put(TimeUnit.MINUTES, "Minutes");
        timeUnitToChineseMap.put(TimeUnit.HOURS, "Hours");
        timeUnitToChineseMap.put(TimeUnit.DAYS, "Days");
            return promptFactory.getDelayTaskPrompt(taskInfo.getDelayTime() + timeUnitToChineseMap.get(taskInfo.getTimeUnit()), taskInfo.getTaskMessage(), DateUtil.now());

    }

    // Start delayed task monitoring when the project starts
    @PostConstruct
    public void init() {
        new Thread(this::executeTasks).start();
    }
}
