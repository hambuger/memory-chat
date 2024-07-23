package com.github.hambuger.memory.chat.memory.other.langchain;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;


/**
 * @author hamburger
 * @since 2024/6/13
 */
public class LangChainRag {

    public interface Assistant {

        String chat(String userMessage);
    }


    public static String generateAnswerWithAiRAG(String question) {

        List<Document> documents = FileSystemDocumentLoader.loadDocuments(toPath(LangChainConstants.DIRECTORY_PATH));

        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        EmbeddingStoreIngestor.ingest(documents, embeddingStore);
        EmbeddingStoreIngestor.builder()
                //.documentTransformer(...)
                //.documentSplitter(...)
                //.textSegmentTransformer(...)
                //.embeddingModel(...)
                .embeddingStore(embeddingStore).build().ingest(documents);

        Assistant assistant = AiServices.builder(Assistant.class).chatLanguageModel(LangChainChat.COMMON_CHAT_MODEL).contentRetriever(EmbeddingStoreContentRetriever.from(embeddingStore)).build();

        String answer = assistant.chat(question);
        return answer;
    }


    private static Path toPath(String fileName) {
        try {
            return Paths.get(new LangChainRag().getClass().getClassLoader().getResource(fileName).toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}
