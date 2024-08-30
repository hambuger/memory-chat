package com.github.hambuger.memory.chat.memory.tools.docparse;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;

import java.util.List;

import lombok.extern.slf4j.Slf4j;


/**
 * @author hamburger
 * @since 2024/8/13
 */
@Slf4j
public class CustomVectorStore extends SimpleVectorStore {

    public CustomVectorStore(EmbeddingModel embeddingModel) {
        super(embeddingModel);
    }


    @Override
    public void add(List<Document> documents) {
        documents.stream().parallel().forEach(document -> {
            document.setEmbedding(this.embeddingModel.embed(document));
            this.store.put(document.getId(), document);
        });
    }
}
