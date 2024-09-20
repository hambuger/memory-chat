package com.github.hambuger.memory.chat.memory.image;

import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.stereotype.Component;

import java.util.Optional;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;


/**
 * @author hamburger
 * @since 2024/7/3
 */
@Slf4j
@Component
public class SpringAiImage {

    @Resource
    private ImageModel imageModel;

    {
        ImageGenerate.registerChannel("openai", this::generateImage);
    }

    public String generateImage(ImageGenerateParam generateParam) {
        ImagePrompt imagePrompt = new ImagePrompt(generateParam.getGenerateText());
        ImageResponse imageResponse = imageModel.call(imagePrompt);
        return Optional.ofNullable(imageResponse).map(ImageResponse::getResult).map(ImageGeneration::getOutput).map(Image::getUrl).orElse(null);
    }

}
