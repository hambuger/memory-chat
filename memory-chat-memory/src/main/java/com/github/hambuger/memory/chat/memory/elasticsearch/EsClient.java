package com.github.hambuger.memory.chat.memory.elasticsearch;

import org.apache.http.HttpHost;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

import jakarta.annotation.PostConstruct;

import static com.github.hambuger.memory.chat.memory.constants.CommonConstants.HTTP;


/**
 * @author hamburger
 */
@Component
public class EsClient {

    @Value("${es.url}")
    private String esUrl;

    @Value("${es.port}")
    private Integer port;

    public static RestHighLevelClient client;


    @PostConstruct
    public void init() {
        client = new RestHighLevelClient(RestClient.builder(new HttpHost(esUrl, port, HTTP)));
    }


    public IndexResponse index(IndexRequest indexRequest) throws IOException {
        return client.index(indexRequest, RequestOptions.DEFAULT);
    }


    public SearchResponse search(SearchRequest searchRequest) throws IOException {
        return client.search(searchRequest, RequestOptions.DEFAULT);
    }

    public UpdateResponse update(UpdateRequest updateRequest) throws IOException {
        return client.update(updateRequest, RequestOptions.DEFAULT);
    }

}
