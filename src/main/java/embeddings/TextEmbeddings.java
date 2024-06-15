package embeddings;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import constants.Constants;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;

import static constants.Constants.TEXT_EMBEDDING_3_SMALL;


/**
 * @author hamburger
 * @since 2024/6/14
 */
public class TextEmbeddings {

    public static List<Float> generateTextEmbeddings(String text) {
        EmbeddingModel model = OpenAiEmbeddingModel.builder().baseUrl(Constants.API_HOST).apiKey(Constants.API_KEY).modelName(TEXT_EMBEDDING_3_SMALL).logRequests(true).logResponses(true).build();
        Response<Embedding> response = model.embed(text);
        return Optional.ofNullable(response).map(Response::content).map(Embedding::vectorAsList).orElse(new ArrayList<>());

    }

}
