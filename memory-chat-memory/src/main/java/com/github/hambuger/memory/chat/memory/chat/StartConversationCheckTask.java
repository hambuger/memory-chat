package com.github.hambuger.memory.chat.memory.chat;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.function.Supplier;

@Slf4j
public class StartConversationCheckTask {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(20);
    private static final ConcurrentHashMap<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();


    public static void startTaskForContact(String taskKey, Supplier<Boolean> checkIfNeedToSendMessage) {
        long initialDelay = 60;
        scheduleTask(taskKey, initialDelay, checkIfNeedToSendMessage);
    }

    private static void scheduleTask(String taskKey, long delay, Supplier<Boolean> checkIfNeedToSendMessage) {
        // Cancel existing task (if it exists)
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
                newDelay = 60L;
            }else {
                newDelay = delay * 2L;
            }

            // Rescheduling tasks
            scheduleTask(taskKey, newDelay, checkIfNeedToSendMessage);
        };
        ScheduledFuture<?> scheduledTask = scheduler.schedule(task, delay, TimeUnit.SECONDS);
        tasks.put(taskKey, scheduledTask);
    }
}
