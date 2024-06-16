package image;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;

import constants.Constants;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.output.Response;

import static dev.ai4j.openai4j.image.ImageModel.*;


/**
 * @author hamburger
 * @since 2024/6/13
 */
public class LangChainImage {

    public static URI generateImageWithOpenai(String prompt) {
        OpenAiImageModel.OpenAiImageModelBuilder modelBuilder =
                OpenAiImageModel.builder().proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(Constants.LOCAL,
                        Constants.PROXY_PORT))).baseUrl(Constants.API_HOST).apiKey(Constants.API_KEY).modelName(DALL_E_3.toString()).size(DALL_E_SIZE_1792_x_1024).logRequests(true).logResponses(true);

        OpenAiImageModel model = modelBuilder.build();

        Response<Image> response = model.generate(prompt);

        URI remoteImage = response.content().url();
        return remoteImage;

    }

}
