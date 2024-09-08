package com.github.hambuger.memory.chat.memory.image;

import com.google.common.collect.Lists;

import com.github.hambuger.memory.chat.memory.learn.LearnProceduralMemory;

import org.springframework.beans.factory.annotation.Value;
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

    @Value("${temp.path}")
    private String tempPath;


    public void capture() {
        String photoPath = tempPath + File.separator + UUID.randomUUID() + "_SHOT.png";
        learnProceduralMemory.invokeSinglePythonFunction(Lists.newArrayList("/capture/capture_photo.py"), "capture_photo", new HashMap<>() {
            {
                put("filename", photoPath);
            }
        });
    }

}
