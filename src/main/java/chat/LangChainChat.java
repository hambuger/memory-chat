package chat;

import constants.Constants;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;


/**
 * @author hamburger
 * @since 2024/6/13
 */
public class LangChainChat {

    public static String generateJsonWithSingleMsgAndPrompt(String msg) {

        OpenAiChatModel model =
                OpenAiChatModel.builder().proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(Constants.LOCAL,
                        Constants.PROXY_PORT))).baseUrl(Constants.API_HOST).apiKey(Constants.API_KEY).temperature(0.0).logRequests(true).logResponses(true).modelName(Constants.MODEL_NAME).responseFormat(
                        "json_object").maxRetries(3).build();
        String json = model.generate(msg);
        return json;
    }


    public static Response<AiMessage> generateMsgWithMsgList(List<ChatMessage> messageList) {

        OpenAiChatModel model =
                OpenAiChatModel.builder().maxRetries(3).proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(Constants.LOCAL,
                        Constants.PROXY_PORT))).baseUrl(Constants.API_HOST).apiKey(Constants.API_KEY).temperature(0.0).logRequests(true).logResponses(true).modelName(Constants.MODEL_NAME).build();
//        messageList.forEach(msg -> System.out.println(msg.text()));
        return model.generate(messageList);
    }

}
