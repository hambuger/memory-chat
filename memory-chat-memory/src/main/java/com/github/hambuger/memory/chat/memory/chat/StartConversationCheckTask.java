package com.github.hambuger.memory.chat.memory.chat;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class StartConversationCheckTask {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(20); // 线程池大小
    private final ConcurrentHashMap<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();


    public void startTaskForContact(String contact, Supplier<Boolean> checkIfNeedToSendMessage, Consumer<String> sendMessage) {
        long initialDelay = 1; // 初始延迟时间为 1 秒
        scheduleTask(contact, initialDelay, checkIfNeedToSendMessage, sendMessage);
    }

    private void scheduleTask(String contact, long delay, Supplier<Boolean> checkIfNeedToSendMessage, Consumer<String> sendMessage) {
        Runnable task = () -> {
            boolean needToSendMessage = checkIfNeedToSendMessage.get();
            long newDelay;
            if (needToSendMessage) {
                sendMessage.accept(contact);
                newDelay = 1L; // 重置延迟时间
            } else {
                newDelay = delay * 2L; // 使用指数退避策略增加延迟时间
            }

            // 重新调度任务
            scheduleTask(contact, newDelay, checkIfNeedToSendMessage, sendMessage);
        };
        ScheduledFuture<?> scheduledTask = scheduler.schedule(task, delay, TimeUnit.SECONDS);
        tasks.put(contact, scheduledTask);
    }

    private boolean checkIfNeedToSendMessage(String contact) {
        // 实现检查逻辑
        double a = Math.random();
        log.info(contact + " random:" + a);
        return a > 0.8; // 模拟检查是否需要发送消息
    }

    private void sendMessage(String contact) {
        // 实现发送消息逻辑
        log.info("Sending message to " + contact);
    }

    public static void main(String[] args) {
        StartConversationCheckTask bot = new StartConversationCheckTask();
        bot.startTaskForContact("contact1", () -> bot.checkIfNeedToSendMessage("contact1"), bot::sendMessage);
        bot.startTaskForContact("contact2", () -> bot.checkIfNeedToSendMessage("contact2"), bot::sendMessage);
    }
}
