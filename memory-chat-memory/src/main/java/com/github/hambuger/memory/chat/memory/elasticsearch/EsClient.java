package com.github.hambuger.memory.chat.memory.elasticsearch;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

import static com.github.hambuger.memory.chat.memory.constants.CommonConstants.HTTP;
import static com.github.hambuger.memory.chat.memory.constants.Constants.ES_PORT;
import static com.github.hambuger.memory.chat.memory.constants.Constants.LOCAL;


/**
 * @author hamburger
 */
public class EsClient {

    public static final RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost(LOCAL, ES_PORT, HTTP)));

}
