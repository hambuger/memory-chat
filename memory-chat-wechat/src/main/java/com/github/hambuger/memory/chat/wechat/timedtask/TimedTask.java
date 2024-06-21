package com.github.hambuger.memory.chat.wechat.timedtask;

import com.github.hambuger.memory.chat.wechat.core.Core;
import com.github.hambuger.memory.chat.wechat.service.LoginService;

import org.springframework.context.annotation.Bean;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;

import jakarta.annotation.Resource;

import lombok.extern.log4j.Log4j2;


/**
 * @author SXS
 * @since 4/13/2021
 */
@Component
@Log4j2
public class TimedTask {
    @Bean
    public AsyncTaskExecutor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("ThreadPoolTaskExecutor-");
        executor.setMaxPoolSize(1);
        executor.setCorePoolSize(1);
        executor.setQueueCapacity(0);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return executor;
    }

    /**
     * 登录服务
     */
    @Resource
    private LoginService loginService;


    /**
     * 30秒获取一次联系人信息 76j00
     */
    @Scheduled(cron = "*/59 * * * * ?")
    public void updateContactTask() {
        if (Core.isAlive()) {
            loginService.webWxGetContact();

            loginService.WebWxBatchGetContact();

        }
    }
}
