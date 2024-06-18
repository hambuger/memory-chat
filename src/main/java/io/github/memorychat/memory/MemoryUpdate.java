package io.github.memorychat.memory;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;

import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.util.Date;

import io.github.memorychat.constants.Constants;
import io.github.memorychat.elasticsearch.EsClient;
import io.github.memorychat.memory.model.MemoryDTO;


/**
 * @author hamburger
 * @since 2024/6/14
 */
public class MemoryUpdate {

    public static boolean updateMemoryAccessTime(String messageId) {

        // 创建要更新的字段和值
        MemoryDTO memoryDTO = new MemoryDTO();
        memoryDTO.setMessageLastAccessTime(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_FORMAT));

        // 创建UpdateRequest
        UpdateRequest updateRequest = new UpdateRequest(Constants.CHAT_MEMORY_INDEX, messageId).doc(JSON.toJSONString(memoryDTO), XContentType.JSON);

        // 执行更新操作
        try {
            EsClient.client.update(updateRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

}
