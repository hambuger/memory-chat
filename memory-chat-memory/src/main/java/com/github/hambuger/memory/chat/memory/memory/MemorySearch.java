package com.github.hambuger.memory.chat.memory.memory;

import com.alibaba.fastjson.JSON;
import com.github.hambuger.memory.chat.memory.chat.dto.ContentTypeEnum;
import com.github.hambuger.memory.chat.memory.chat.dto.CreatorEnum;
import com.github.hambuger.memory.chat.memory.elasticsearch.EsClient;
import com.github.hambuger.memory.chat.memory.embeddings.SpringAiEmbeddings;
import com.github.hambuger.memory.chat.memory.memory.model.MemoryDTO;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.lucene.search.function.FunctionScoreQuery;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.functionscore.FieldValueFactorFunctionBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.GaussDecayFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScriptScoreFunctionBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;


/**
 * @author hamburger
 * @since 2024/6/13
 */
@Slf4j
@Component
public class MemorySearch {

    @Resource
    private SpringAiEmbeddings springAiEmbeddings;

    @Resource
    private EsClient esClient;

    @Value("${chatMemoryIndex}")
    private String chatMemoryIndex;


    public List<MemoryDTO> searchRelationMemory(String ownerId, String creatorId, String content) {
        List<Double> contentVector = springAiEmbeddings.generateTextEmbeddings(content);
        BoolQueryBuilder mustQuery = QueryBuilders.boolQuery();
        // 排除图片和系统消息
        mustQuery.must(new TermQueryBuilder("messageContentType", ContentTypeEnum.TEXT.getType()));
        if (StringUtils.isNotBlank(ownerId)) {
            // 数据隔离
            mustQuery.must(new TermQueryBuilder("messageOwnerId", ownerId));
        }
        if (StringUtils.isNotBlank(creatorId)) {
            mustQuery.must(new TermQueryBuilder("messageCreatorId", creatorId));
        }else {
            // 排除AI回复
            mustQuery.mustNot(new TermQueryBuilder("messageCreatorId", CreatorEnum.Andrew.getUserId()));
        }
        // 构建查询体
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(6);
        searchSourceBuilder.query(QueryBuilders.functionScoreQuery(QueryBuilders.boolQuery().must(mustQuery),
                new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{new FunctionScoreQueryBuilder.FilterFunctionBuilder(QueryBuilders.matchQuery("messageContent", content),
                        new ScriptScoreFunctionBuilder(new Script(ScriptType.INLINE, "painless", "_score / (1 + _score)", Collections.emptyMap()))),
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder(QueryBuilders.matchAllQuery(), new GaussDecayFunctionBuilder("messageLastAccessTime", "now", "24h", "1h", 0.5)),
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder(new FieldValueFactorFunctionBuilder("messageImportanceScore")),
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder(new ScriptScoreFunctionBuilder(new Script(ScriptType.INLINE, "painless", "1 / (1 + Math.exp(-1.0 * " + "doc" +
                                "['memoryLeafDepth'].value))", Collections.emptyMap()))), new FunctionScoreQueryBuilder.FilterFunctionBuilder(QueryBuilders.matchAllQuery(),
                        new ScriptScoreFunctionBuilder(new Script(ScriptType.INLINE, "painless", "double score = (cosineSimilarity(params.query_vector, 'messageContentVector') + 1.0); return score "
                                + "> 0.5 " + "? 10 + score : 0;", new HashMap() {{
            put("query_vector", contentVector);
        }})))}).scoreMode(FunctionScoreQuery.ScoreMode.SUM).boostMode(CombineFunction.REPLACE).setMinScore(10));

        SearchRequest searchRequest = new SearchRequest(chatMemoryIndex);
        searchRequest.source(searchSourceBuilder);
        try {
            // 执行查询
            SearchResponse searchResponse = esClient.search(searchRequest);

            return Arrays.stream(searchResponse.getHits().getHits()).map(hit -> JSON.parseObject(hit.getSourceAsString(), MemoryDTO.class)).collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

}
