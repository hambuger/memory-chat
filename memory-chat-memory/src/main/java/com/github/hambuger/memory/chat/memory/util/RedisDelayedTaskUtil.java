package com.github.hambuger.memory.chat.memory.util;

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

    // 添加延迟任务
    public void addTask(String message, long delay, TimeUnit timeUnit) {
        // 将自定义时间单位转换为秒
        long delayInSeconds = timeUnit.toSeconds(delay);
        long executionTime = Instant.now().getEpochSecond() + delayInSeconds;
        commonRedisTemplate.opsForZSet().add(DELAYED_TASK_KEY, message, executionTime);
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
