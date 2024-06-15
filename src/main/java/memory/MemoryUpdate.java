package memory;

import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import constants.Constants;
import elasticsearch.EsClient;


/**
 * @author hamburger
 * @since 2024/6/14
 */
public class MemoryUpdate {

    public static boolean updateMemoryAccessTime(String messageId) {

        // 创建要更新的字段和值的Map
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("messageCreateAt", new Date());

        // 创建UpdateRequest
        UpdateRequest updateRequest = new UpdateRequest(Constants.CHAT_MEMORY_INDEX, messageId).doc(jsonMap);

        // 执行更新操作
        try {
            EsClient.client.update(updateRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

}
