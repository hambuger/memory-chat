package com.github.hambuger.memory.chat.memory.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.other.functionCall.aop.FunctionCallRegistry;

import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.stereotype.Component;

import java.util.Optional;

import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;


/**
 * @author hanjiabao
 * @since 2024/7/3
 */
@Slf4j
@Component
public class SpringAiImage {

    @Resource
    private ImageModel imageModel;


    @Data
    @AllArgsConstructor
    public static class ImageGenerateParam {

        @JsonPropertyDescription("生成图片提示词")
        @JsonProperty(required = true)
        private String generateText;
    }


    @FunctionCallRegistry(functionDesc = "生成图片", scene = {ChatSceneEnum.NORMAL_USER, ChatSceneEnum.NORMAL_GROUP})
    public String generateImage(ImageGenerateParam generateParam) {
        ImagePrompt imagePrompt = new ImagePrompt(generateParam.generateText);
        ImageResponse imageResponse = imageModel.call(imagePrompt);
        return Optional.ofNullable(imageResponse).map(ImageResponse::getResult).map(ImageGeneration::getOutput).map(Image::getUrl).orElse(null);
    }

}
