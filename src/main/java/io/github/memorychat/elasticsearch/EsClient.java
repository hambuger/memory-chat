package io.github.memorychat.elasticsearch;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

import static io.github.memorychat.constants.CommonConstants.HTTP;
import static io.github.memorychat.constants.Constants.ES_PORT;
import static io.github.memorychat.constants.Constants.LOCAL;


/**
 * @author hamburger
 */
public class EsClient {

    public static final RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost(LOCAL, ES_PORT, HTTP)));

}
