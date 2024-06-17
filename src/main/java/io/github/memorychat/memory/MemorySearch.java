package io.github.memorychat.memory;

import com.alibaba.fastjson.JSON;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import io.github.memorychat.elasticsearch.EsClient;
import io.github.memorychat.embeddings.TextEmbeddings;
import io.github.memorychat.memory.model.MemoryDTO;

import static io.github.memorychat.constants.Constants.CHAT_MEMORY_INDEX;


/**
 * @author hamburger
 * @since 2024/6/13
 */
public class MemorySearch {

    public static List<MemoryDTO> searchRelationMemory(String ownerId, String content) {
        List<Float> contentVector = TextEmbeddings.generateTextEmbeddings(content);
        BoolQueryBuilder mustQuery = QueryBuilders.boolQuery();
        mustQuery.must(new TermQueryBuilder("messageContentType", "TEXT"));
        if (StringUtils.isNotBlank(ownerId)) {
            mustQuery.must(new TermQueryBuilder("messageCreatorId", ownerId));
        }
        // 构建查询体
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(3);
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

        SearchRequest searchRequest = new SearchRequest(CHAT_MEMORY_INDEX);
        searchRequest.source(searchSourceBuilder);
        try {
            // 执行查询
            SearchResponse searchResponse = EsClient.client.search(searchRequest, RequestOptions.DEFAULT);

            return Arrays.stream(searchResponse.getHits().getHits()).map(hit -> JSON.parseObject(hit.getSourceAsString(), MemoryDTO.class)).collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

}
