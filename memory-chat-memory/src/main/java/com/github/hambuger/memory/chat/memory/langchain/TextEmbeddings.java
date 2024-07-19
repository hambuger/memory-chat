package com.github.hambuger.memory.chat.memory.langchain;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;

import static com.github.hambuger.memory.chat.memory.langchain.LangChainConstants.TEXT_EMBEDDING_3_SMALL;


/**
 * @author hamburger
 * @since 2024/6/14
 */
public class TextEmbeddings {

    public static EmbeddingModel EMBEDDINGS_MODEL =
            OpenAiEmbeddingModel.builder().proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(LangChainConstants.LOCAL, LangChainConstants.PROXY_PORT))).baseUrl(LangChainConstants.API_HOST).apiKey(LangChainConstants.API_KEY).modelName(TEXT_EMBEDDING_3_SMALL).logRequests(true).logResponses(true).build();


    public static List<Float> generateTextEmbeddings(String text) {
        Response<Embedding> response = EMBEDDINGS_MODEL.embed(text);
        return Optional.ofNullable(response).map(Response::content).map(Embedding::vectorAsList).orElse(new ArrayList<>());

    }


    public static void main(String[] args) {
        generateTextEmbeddings("test");
    }

}
