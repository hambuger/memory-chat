package com.github.hambuger.memory.chat.memory.memory;

import com.google.common.base.Objects;

import com.alibaba.fastjson.JSON;
import com.github.hambuger.memory.chat.memory.chat.ChatCompletionsApi;
import com.github.hambuger.memory.chat.memory.chat.dto.ContentTypeEnum;
import com.github.hambuger.memory.chat.memory.chat.dto.CreatorEnum;
import com.github.hambuger.memory.chat.memory.constants.CommonConstants;
import com.github.hambuger.memory.chat.memory.constants.MemoryChatConstants;
import com.github.hambuger.memory.chat.memory.elasticsearch.EsClient;
import com.github.hambuger.memory.chat.memory.embeddings.SpringAiEmbeddings;
import com.github.hambuger.memory.chat.memory.memory.model.MemoryDTO;
import com.github.hambuger.memory.chat.memory.token.TokenCalculation;
import com.github.hambuger.memory.chat.memory.util.IdUtil;
import com.github.hambuger.memory.chat.memory.util.RedisUtil;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;


/**
 * @author hamburger
 * @since 2024/6/13
 */
@Slf4j
@Component
public class MemoryInsert {

    @Resource
    private SpringAiEmbeddings springAiEmbeddings;

    @Resource
    private EsClient esClient;

    @Resource
    private MemoryImportantScore memoryImportantScore;

    @Resource
    private MemoryReflection memoryReflection;

    @Resource
    private RedisUtil redisUtil;

    @Value("${spring.ai.openai.chat.options.model}")
    private String modelName;

    @Value("${reflection.tokenLimit}")
    private Integer reflectionTokenLimit;

    @Value("${chatMemoryIndex}")
    private String chatMemoryIndex;


    public Boolean insertNewMemory(MemoryDTO memoryDTO) {
        if (StringUtils.isBlank(memoryDTO.getMessageId())) {
            memoryDTO.setMessageId(IdUtil.generateUniqueId());
        }
        boolean userMsgFlag = StringUtils.equals(memoryDTO.getAiResponseFlag(), CommonConstants.NO_STR);
        if (!userMsgFlag) {
            String msgListKey = memoryDTO.getMessageOwnerId() + CommonConstants.DOUBLE_COLON + (Objects.equal(memoryDTO.getAiResponseFlag(), CommonConstants.NO_STR) ? memoryDTO.getMessageCreatorId() :
                    memoryDTO.getMessageReceiveId()) + MemoryChatConstants.MSG_LIST_KEY_SUFFIX;
            redisUtil.addMsg(msgListKey,
                    MemoryDTO.builder().messageId(memoryDTO.getMessageId()).messageCreateAt(memoryDTO.getMessageCreateAt()).groupMsgFlag(memoryDTO.getGroupMsgFlag()).messageContentType(memoryDTO.getMessageContentType()).aiResponseFlag(memoryDTO.getAiResponseFlag()).messageContent(memoryDTO.getMessageContent()).build());
        }
        boolean textMsgFlag = StringUtils.equals(memoryDTO.getMessageContentType(), ContentTypeEnum.TEXT.getType());
        // 生成重要性分数
        String messageContent = memoryDTO.getMessageContent();
        if (textMsgFlag && userMsgFlag) {
            Double score = memoryImportantScore.generateImportantScore(messageContent);
            memoryDTO.setMessageImportanceScore(score);
            // 生成消息向量
            List<Double> vector = springAiEmbeddings.generateTextEmbeddings(messageContent);
            memoryDTO.setMessageContentVector(vector);
        }
        if (!userMsgFlag && ChatCompletionsApi.checkLastMessageId(memoryDTO)) {
            return false;
        }
        IndexRequest indexRequest = new IndexRequest(chatMemoryIndex).id(memoryDTO.getMessageId()).source(JSON.toJSONString(memoryDTO), XContentType.JSON);
        try {
            esClient.index(indexRequest);
        } catch (IOException e) {
            log.error("insert memory error", e);
        }
        // 检查是否需要提炼
        if (textMsgFlag) {
            String depthLeafCountKey = memoryDTO.getMessageOwnerId() + CommonConstants.DOUBLE_COLON + memoryDTO.getMemoryLeafDepth();
            String depthLeafListKey = memoryDTO.getMessageOwnerId() + MemoryChatConstants.DEPTH_LEAF_LIST_KEY_MID + memoryDTO.getMemoryLeafDepth();
            redisUtil.incrBy(depthLeafCountKey, memoryDTO.getUseToken());
            String jsonInfo = getAiUseJsonInfo(memoryDTO);
            redisUtil.addElement(depthLeafListKey, jsonInfo);
            boolean botFlag = StringUtils.equals(memoryDTO.getMessageCreatorType(), CreatorEnum.Andrew.getType()) || StringUtils.equals(memoryDTO.getMessageCreatorType(), CreatorEnum.REFLECTION.getType());
            checkAndInsertDepthLeafReflection(memoryDTO.getMemoryLeafDepth(), depthLeafCountKey, depthLeafListKey, memoryDTO.getMessageOwnerId(), memoryDTO.getMessageOwnerName(),
                    memoryDTO.getMessageOwnerType(),
                    botFlag ? memoryDTO.getMessageReceiveId() : memoryDTO.getMessageCreatorId(),
                    botFlag ? memoryDTO.getMessageReceiveName() : memoryDTO.getMessageCreatorName(),
                    botFlag ? memoryDTO.getMessageReceiveType() : memoryDTO.getMessageCreatorType());
        }
        return true;
    }


    private static String getAiUseJsonInfo(MemoryDTO memoryDTO) {
        MemoryDTO newMemoryDTO = new MemoryDTO();
        newMemoryDTO.setMessageId(memoryDTO.getMessageId());
        newMemoryDTO.setMessageContent(memoryDTO.getMessageContent());
        newMemoryDTO.setMessageCreatorName(Optional.ofNullable(memoryDTO.getRealCreatorName()).orElse(memoryDTO.getMessageCreatorName()));
        newMemoryDTO.setMessageCreateAt(memoryDTO.getMessageCreateAt());
        newMemoryDTO.setMessageImportanceScore(memoryDTO.getMessageImportanceScore());
        return JSON.toJSONString(newMemoryDTO);
    }


    private void checkAndInsertDepthLeafReflection(Integer leafDepth, String depthLeafKey, String depthLeafListKey, String ownerId, String ownerName, String ownerType
            , String receiveId, String receiveName, String receiveType) {
        // 总token提炼限制
        if (redisUtil.get(depthLeafKey) < reflectionTokenLimit) {
            return;
        }
        List<MemoryReflection.ReflectionResult.Reflection> reflectionList = memoryReflection.extractReflectionFromMessages(redisUtil.getList(depthLeafListKey));
        redisUtil.reset(depthLeafKey);
        redisUtil.reset(depthLeafListKey);
        if (CollectionUtils.isEmpty(reflectionList)) {
            return;
        }
        for (MemoryReflection.ReflectionResult.Reflection reflection : reflectionList) {
            String reflectionText = reflection.getText();
            List<String> parentIdList = reflection.getP_ids();
            MemoryDTO memoryDTO =
                    MemoryDTO.builder().messageCreatorId(CreatorEnum.REFLECTION.getUserId()).messageCreatorName(CreatorEnum.REFLECTION.getUserName())
                            .messageCreatorType(CreatorEnum.REFLECTION.getType())
                            .messageContentType(ContentTypeEnum.TEXT.getType())
                            .messageParentIds(parentIdList).messageContent(reflectionText).aiResponseFlag(CommonConstants.NO_STR)
                            .messageCreateAt(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_FORMAT))
                            .messageReceiveId(receiveId).messageReceiveName(receiveName).messageReceiveType(receiveType)
                            .messageOwnerId(ownerId).messageOwnerName(ownerName).messageOwnerType(ownerType)
                            .memoryLeafDepth(leafDepth + 1).useToken(new TokenCalculation(modelName).getMessageTextTokenCount(reflectionText)).build();
            insertNewMemory(memoryDTO);
        }
    }

}
