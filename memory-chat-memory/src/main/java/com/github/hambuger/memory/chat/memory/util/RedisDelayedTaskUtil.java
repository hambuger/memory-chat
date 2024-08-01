package com.github.hambuger.memory.chat.memory.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.hambuger.memory.chat.memory.chat.dto.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.functionCall.aop.FunctionCallRegistry;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RedisDelayedTaskUtil {

    @Autowired
    private RedisTemplate<String, Object> commonRedisTemplate;

    private static final String DELAYED_TASK_KEY = "delayedTasks";

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TaskInfo {

        @JsonPropertyDescription("延迟任务的内容描述")
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
//    @FunctionCallRegistry(functionDesc = "添加一个任务，以便在未来时间处理", scene = {ChatSceneEnum.NORMAL_USER, ChatSceneEnum.NORMAL_GROUP, ChatSceneEnum.TASK})
    public void addTask(TaskInfo taskInfo) {
        // 将自定义时间单位转换为秒
        long delayInSeconds = taskInfo.getTimeUnit().toSeconds(taskInfo.getDelayTime());
        long executionTime = Instant.now().getEpochSecond() + delayInSeconds;
        commonRedisTemplate.opsForZSet().add(DELAYED_TASK_KEY, taskInfo.getTaskMessage(), executionTime);
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
                commonRedisTemplate.opsForZSet().remove(DELAYED_TASK_KEY, taskObj);
            }
        }
    }

    // 在项目启动时启动延迟任务监听
//    @PostConstruct
    public void init() {
        new Thread(this::executeTasks).start();
    }
}
