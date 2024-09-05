package com.github.hambuger.memory.chat.memory.emoji;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.hambuger.memory.chat.memory.chat.SpringAiChat;
import com.github.hambuger.memory.chat.memory.other.embeddings.SpringAiEmbeddings;
import com.github.hambuger.memory.chat.memory.other.prompt.PromptFactory;
import com.github.hambuger.memory.chat.memory.other.util.EsClient;
import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.utils.StringUtils;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;


/**
 * @author hanjiabao
 * @since 2024/9/4
 */
@Slf4j
@Component
public class SelfEmoji {

    @Resource
    private SpringAiChat springAiChat;

    @Resource
    private EsClient esClient;

    @Resource
    private PromptFactory promptFactory;

    @Resource
    private SpringAiEmbeddings springAiEmbeddings;

    public static final String EMOJI_INDEX = "self_emoji";


    @Data
    @NoArgsConstructor
    public static class EmojiExtraWord {

        @JsonPropertyDescription("表情风格")
        @JsonProperty(required = true)
        private String emojiStyle;

        @JsonPropertyDescription("表情类别")
        @JsonProperty(required = true)
        private String emojiCategory;

        @JsonPropertyDescription("表情标题")
        @JsonProperty(required = true)
        private String emojiTitle;

        @JsonPropertyDescription("情感标签")
        @JsonProperty(required = true)
        private String emojiEmotionTag;

        @JsonPropertyDescription("视觉特征描述")
        @JsonProperty(required = true)
        private String emojiVisualFeatureDescription;

        @JsonPropertyDescription("使用场景或语境")
        @JsonProperty(required = true)
        private String emojiUsageContext;
    }


    public void getEmojiExtra(String emojiUrl) {
        String prompt = promptFactory.getEmojiExtraPrompt();
        List<OpenAiApi.ChatCompletionMessage.MediaContent> contentList = new ArrayList<>();
        contentList.add(new OpenAiApi.ChatCompletionMessage.MediaContent(new OpenAiApi.ChatCompletionMessage.MediaContent.ImageUrl(emojiUrl)));
        List<OpenAiApi.ChatCompletionMessage> messages = Lists.newArrayList(new OpenAiApi.ChatCompletionMessage(prompt, OpenAiApi.ChatCompletionMessage.Role.SYSTEM), new OpenAiApi.ChatCompletionMessage(contentList, OpenAiApi.ChatCompletionMessage.Role.USER));
        OpenAiApi.ChatCompletion chatCompletion = springAiChat.generateMsgWithMsgList(messages, EmojiExtraWord.class, "EmojiExtraWord");
        String content = Optional.ofNullable(chatCompletion).map(OpenAiApi.ChatCompletion::choices).map(list -> list.get(0)).map(OpenAiApi.ChatCompletion.Choice::message).map(OpenAiApi.ChatCompletionMessage::content).orElse(null);
        if (StringUtils.isBlank(content)) {
            return;
        }
        JSONObject emojiExtraWord = JSON.parseObject(content);
        emojiExtraWord.put("emojiCategory_vector", springAiEmbeddings.generateTextEmbeddings(emojiExtraWord.getString("emojiCategory")));
        emojiExtraWord.put("emojiEmotionTag_vector", springAiEmbeddings.generateTextEmbeddings(emojiExtraWord.getString("emojiEmotionTag")));
        emojiExtraWord.put("emojiStyle_vector", springAiEmbeddings.generateTextEmbeddings(emojiExtraWord.getString("emojiStyle")));
        emojiExtraWord.put("emojiTitle_vector", springAiEmbeddings.generateTextEmbeddings(emojiExtraWord.getString("emojiTitle")));
        emojiExtraWord.put("emojiUsageContext_vector", springAiEmbeddings.generateTextEmbeddings(emojiExtraWord.getString("emojiUsageContext")));
        emojiExtraWord.put("emojiVisualFeatureDescription_vector", springAiEmbeddings.generateTextEmbeddings(emojiExtraWord.getString("emojiVisualFeatureDescription")));
        emojiExtraWord.put("emojiUrl", emojiUrl);
        IndexRequest indexRequest = new IndexRequest(EMOJI_INDEX).id(UUID.randomUUID().toString()).source(JSON.toJSONString(emojiExtraWord), XContentType.JSON);
        try {
            esClient.index(indexRequest);
        } catch (IOException e) {
            log.error("insert emojiExtra error", e);
        }
    }

    public String queryEmoji(String queryWord) {
        List<Double> embeddings = springAiEmbeddings.generateTextEmbeddings(queryWord);
        Map<String, Object> params = new HashMap<>();
        params.put("query_vector", embeddings);
        String script = "double score = 0; " +
                "score += cosineSimilarity(params.query_vector, 'emojiStyle_vector'); " +
                "score += cosineSimilarity(params.query_vector, 'emojiCategory_vector'); " +
                "score += cosineSimilarity(params.query_vector, 'emojiTitle_vector'); " +
                "score += cosineSimilarity(params.query_vector, 'emojiEmotionTag_vector'); " +
                "score += cosineSimilarity(params.query_vector, 'emojiVisualFeatureDescription_vector'); " +
                "score += cosineSimilarity(params.query_vector, 'emojiUsageContext_vector'); " +
                "return score;";
        Script scriptObject = new Script(ScriptType.INLINE, "painless", script, params);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(QueryBuilders.scriptScoreQuery(QueryBuilders.matchAllQuery(), scriptObject));

        SearchRequest searchRequest = new SearchRequest(EMOJI_INDEX);
        searchSourceBuilder.size(1);
        searchRequest.source(searchSourceBuilder);
        try {
            SearchResponse searchResponse = esClient.search(searchRequest);
            return Arrays.stream(searchResponse.getHits().getHits()).toList().get(0).getSourceAsMap().get("emojiUrl").toString();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return null;
    }


}
