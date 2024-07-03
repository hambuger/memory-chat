package com.github.hambuger.memory.chat.memory.embeddings;

import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.stereotype.Component;

import java.util.List;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;


/**
 * @author hanjiabao
 * @since 2024/7/3
 */
@Slf4j
@Component
public class SpringAiEmbeddings {

    @Resource
    private OpenAiEmbeddingModel embeddingModel;


    public List<Double> generateTextEmbeddings(String text) {
        return embeddingModel.embed(text);
    }

}
