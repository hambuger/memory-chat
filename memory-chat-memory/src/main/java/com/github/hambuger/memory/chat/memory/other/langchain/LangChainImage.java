package com.github.hambuger.memory.chat.memory.other.langchain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.output.Response;
import lombok.AllArgsConstructor;
import lombok.Data;

import static dev.ai4j.openai4j.image.ImageModel.DALL_E_3;
import static dev.ai4j.openai4j.image.ImageModel.DALL_E_SIZE_1792_x_1024;


/**
 * @author hamburger
 * @since 2024/6/13
 */
@Component
public class LangChainImage {

    public static OpenAiImageModel IMAGE_MODEL =
            OpenAiImageModel.builder().proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(LangChainConstants.LOCAL, LangChainConstants.PROXY_PORT))).baseUrl(LangChainConstants.API_HOST).apiKey(LangChainConstants.API_KEY).modelName(DALL_E_3.toString()).size(DALL_E_SIZE_1792_x_1024).logRequests(true).logResponses(true).build();


    @Data
    @AllArgsConstructor
    public static class ImageGenerateParam {

        @JsonPropertyDescription("生成图片提示词")
        @JsonProperty(required = true)
        private String generateText;
    }


//    @FunctionCallRegistry(functionDesc = "生成图片")
    public static String generateImage(ImageGenerateParam generateParam) {

        Response<Image> response = IMAGE_MODEL.generate(generateParam.getGenerateText());

        URI remoteImage = response.content().url();
        return remoteImage.toString();

    }

}
