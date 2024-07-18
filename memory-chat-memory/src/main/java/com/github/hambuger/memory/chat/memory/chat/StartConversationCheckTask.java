package com.github.hambuger.memory.chat.memory.chat;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.function.Supplier;

@Slf4j
public class StartConversationCheckTask {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(20); // 线程池大小
    private static final ConcurrentHashMap<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();


    public static void startTaskForContact(String taskKey, Supplier<Boolean> checkIfNeedToSendMessage) {
        long initialDelay = 60; // 初始延迟时间为 60秒
        scheduleTask(taskKey, initialDelay, checkIfNeedToSendMessage);
    }

    private static void scheduleTask(String taskKey, long delay, Supplier<Boolean> checkIfNeedToSendMessage) {
        // 取消现有任务（如果存在）
        ScheduledFuture<?> existingTask = tasks.get(taskKey);
        if (existingTask != null && !existingTask.isDone()) {
            existingTask.cancel(false);
        }
        Runnable task = () -> {
            boolean needToSendMessage = false;
            try {
                needToSendMessage = checkIfNeedToSendMessage.get();
            } catch (Exception e) {
                log.error("scheduleTask error", e);
            }
            log.info("taskKey:{}, checkIfNeedToSendMessage:{}", taskKey, needToSendMessage);
            long newDelay;
            if (needToSendMessage) {
                newDelay = 60L; // 重置延迟时间
            }else {
                newDelay = delay * 2L; // 使用指数退避策略增加延迟时间
            }

            // 重新调度任务
            scheduleTask(taskKey, newDelay, checkIfNeedToSendMessage);
        };
        ScheduledFuture<?> scheduledTask = scheduler.schedule(task, delay, TimeUnit.SECONDS);
        tasks.put(taskKey, scheduledTask);
    }
}
