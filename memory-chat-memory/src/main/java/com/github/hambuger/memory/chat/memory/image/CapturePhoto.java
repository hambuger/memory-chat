package com.github.hambuger.memory.chat.memory.image;

import com.google.common.collect.Lists;

import com.github.hambuger.memory.chat.memory.learn.LearnProceduralMemory;
import com.github.hambuger.memory.chat.wechat.configuration.WechatConfiguration;

import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

import jakarta.annotation.Resource;


/**
 * @author hamburger
 * @since 2024/8/27
 */
@Component
public class CapturePhoto {

    @Resource
    private LearnProceduralMemory learnProceduralMemory;

    @Resource
    private WechatConfiguration config;


    public void capture() {
        String photoPath = config.getBasePath() + File.separator + UUID.randomUUID() + "_SHOT.png";
        learnProceduralMemory.invokeSinglePythonFunction(Lists.newArrayList("/capture/capture_photo.py"), "capture_photo", new HashMap<>() {
            {
                put("filename", photoPath);
            }
        });
    }

}
