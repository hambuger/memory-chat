package com.github.hambuger.memory.chat.memory.memory.update;

import com.alibaba.fastjson.JSON;
import com.github.hambuger.memory.chat.memory.other.util.EsClient;
import com.github.hambuger.memory.chat.memory.memory.model.MemoryDTO;

import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;


/**
 * @author hamburger
 * @since 2024/6/14
 */
@Slf4j
@Component
public class MemoryUpdate {

    @Resource
    private EsClient esClient;

    @Value("${chatMemoryIndex}")
    private String chatMemoryIndex;


    public boolean updateMemoryAccessTime(String messageId) {

        // Create fields and values to update
        MemoryDTO memoryDTO = new MemoryDTO();
        memoryDTO.setMessageLastAccessTime(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_FORMAT));

        // UpdateRequest
        UpdateRequest updateRequest = new UpdateRequest(chatMemoryIndex, messageId).doc(JSON.toJSONString(memoryDTO), XContentType.JSON);

        // Perform update operation
        try {
            esClient.update(updateRequest);
        } catch (IOException e) {
            log.warn("updateMemoryAccessTime error", e);
        }
        return true;
    }

}
