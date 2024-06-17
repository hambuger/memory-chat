package io.github.memorychat.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;

import io.github.memorychat.constants.Constants;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.output.Response;
import io.github.memorychat.functionCall.aop.FunctionCallRegistry;
import lombok.AllArgsConstructor;
import lombok.Data;

import static dev.ai4j.openai4j.image.ImageModel.*;


/**
 * @author hamburger
 * @since 2024/6/13
 */
@Component
public class LangChainImage {

    @Data
    @AllArgsConstructor
    public static class ImageGenerateParam {

        @JsonPropertyDescription("生成图片提示词")
        @JsonProperty(required = true)
        private String generateText;
    }


    @FunctionCallRegistry(functionDesc = "生成图片")
    public static String generateImage(ImageGenerateParam generateParam) {
        OpenAiImageModel.OpenAiImageModelBuilder modelBuilder =
                OpenAiImageModel.builder().proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(Constants.LOCAL, Constants.PROXY_PORT))).baseUrl(Constants.API_HOST).apiKey(Constants.API_KEY).modelName(DALL_E_3.toString()).size(DALL_E_SIZE_1792_x_1024).logRequests(true).logResponses(true);

        OpenAiImageModel model = modelBuilder.build();

        Response<Image> response = model.generate(generateParam.getGenerateText());

        URI remoteImage = response.content().url();
        return remoteImage.toString();

    }

}
