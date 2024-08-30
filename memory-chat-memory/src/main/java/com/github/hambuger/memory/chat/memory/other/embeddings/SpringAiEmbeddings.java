package com.github.hambuger.memory.chat.memory.other.embeddings;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


/**
 * @author hamburger
 * @since 2024/7/3
 */
@Slf4j
@Component
public class SpringAiEmbeddings {

    @Resource
    private OpenAiEmbeddingModel embeddingModel;

    public OpenAiEmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }


    public List<Double> generateTextEmbeddings(String text) {
        return convertFloatArrayToList(embeddingModel.embed(text));
    }

    public static List<Double> convertFloatArrayToList(float[] floatArray) {
        List<Double> doubleList = new ArrayList<>();
        for (float value : floatArray) {
            doubleList.add((double) value);
        }
        return doubleList;
    }

}
