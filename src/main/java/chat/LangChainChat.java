package chat;

import java.util.List;

import constants.Constants;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;


/**
 * @author hamburger
 * @since 2024/6/13
 */
public class LangChainChat {

    public static String generateJsonWithSingleMsgAndPrompt(String msg) {

        OpenAiChatModel model =
                OpenAiChatModel.builder().baseUrl(Constants.API_HOST).apiKey(Constants.API_KEY).temperature(0.0).logRequests(true).logResponses(true).modelName(Constants.MODEL_NAME).responseFormat(
                        "json_object").maxRetries(3).build();
        String json = model.generate(msg);
        return json;
    }


    public static Response<AiMessage> generateMsgWithMsgList(List<ChatMessage> messageList) {

        OpenAiChatModel model =
                OpenAiChatModel.builder().maxRetries(3).baseUrl(Constants.API_HOST).apiKey(Constants.API_KEY).temperature(0.0).logRequests(true).logResponses(true).modelName(Constants.MODEL_NAME).build();
        return model.generate(messageList);
    }

}
