package io.github.memorychat.memory;

import com.google.common.base.Objects;

import com.alibaba.fastjson.JSON;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import dev.langchain4j.data.message.UserMessage;
import io.github.memorychat.chat.dto.ContentTypeEnum;
import io.github.memorychat.chat.dto.CreatorEnum;
import io.github.memorychat.elasticsearch.EsClient;
import io.github.memorychat.embeddings.TextEmbeddings;
import io.github.memorychat.memory.model.MemoryDTO;
import io.github.memorychat.util.IdUtil;
import io.github.memorychat.util.OpenAiTokenizerUtil;
import io.github.memorychat.util.RedisLikeCounter;
import io.github.memorychat.wechat.ChatCompletionsApi;

import static io.github.memorychat.constants.CommonConstants.DOUBLE_COLON;
import static io.github.memorychat.constants.CommonConstants.NO_STR;
import static io.github.memorychat.constants.Constants.AI_CREATOR_ID;
import static io.github.memorychat.constants.Constants.AI_CREATOR_NAME;
import static io.github.memorychat.constants.Constants.CHAT_MEMORY_INDEX;
import static io.github.memorychat.constants.Constants.DEPTH_LEAF_LIST_KEY_MID;
import static io.github.memorychat.constants.Constants.MSG_LIST_KEY_SUFFIX;
import static io.github.memorychat.constants.Constants.REFLECTION_TOKEN_LIMIT;


/**
 * @author hamburger
 * @since 2024/6/13
 */
public class MemoryInsert {


    public static Boolean insertNewMemory(MemoryDTO memoryDTO) {
        if (StringUtils.isBlank(memoryDTO.getMessageId())) {
            memoryDTO.setMessageId(IdUtil.generateUniqueId());
        }
        boolean userMsgFlag = StringUtils.equals(memoryDTO.getAiResponseFlag(), NO_STR);
        if (!userMsgFlag) {
            String msgListKey = memoryDTO.getMessageOwnerId() + DOUBLE_COLON + (Objects.equal(memoryDTO.getAiResponseFlag(), NO_STR) ? memoryDTO.getMessageCreatorId() :
                    memoryDTO.getMessageReceiveId()) + MSG_LIST_KEY_SUFFIX;
            RedisLikeCounter.addMsg(msgListKey,
                    MemoryDTO.builder().messageId(memoryDTO.getMessageId()).messageContentType(memoryDTO.getMessageContentType()).aiResponseFlag(memoryDTO.getAiResponseFlag()).messageContent(memoryDTO.getMessageContent()).build());
        }
        boolean textMsgFlag = StringUtils.equals(memoryDTO.getMessageContentType(), ContentTypeEnum.TEXT.getType());
        // 生成重要性分数
        String messageContent = memoryDTO.getMessageContent();
        if (textMsgFlag && userMsgFlag) {
            Double score = MemoryImportantScore.generateImportantScore(messageContent);
            memoryDTO.setMessageImportanceScore(score);
            // 生成消息向量
            List<Float> vector = TextEmbeddings.generateTextEmbeddings(messageContent);
            memoryDTO.setMessageContentVector(vector);
        }
        if (!userMsgFlag && ChatCompletionsApi.checkLastMessageId(memoryDTO)) {
            return false;
        }
        IndexRequest indexRequest = new IndexRequest(CHAT_MEMORY_INDEX).id(memoryDTO.getMessageId()).source(JSON.toJSONString(memoryDTO), XContentType.JSON);
        try {
            EsClient.client.index(indexRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 检查是否需要提炼
        if (userMsgFlag && textMsgFlag) {
            String depthLeafCountKey = memoryDTO.getMessageOwnerId() + DOUBLE_COLON + memoryDTO.getMemoryLeafDepth();
            String depthLeafListKey = memoryDTO.getMessageOwnerId() + DEPTH_LEAF_LIST_KEY_MID + memoryDTO.getMemoryLeafDepth();
            RedisLikeCounter.incrBy(depthLeafCountKey, memoryDTO.getUseToken());
            String jsonInfo = getAiUseJsonInfo(memoryDTO);
            RedisLikeCounter.addElement(depthLeafListKey, jsonInfo);
            checkAndInsertDepthLeafReflection(memoryDTO.getMemoryLeafDepth(), depthLeafCountKey, depthLeafListKey, memoryDTO.getMessageOwnerId(), memoryDTO.getMessageOwnerName(),
                    memoryDTO.getMessageOwnerType());
        }
        return true;
    }


    private static String getAiUseJsonInfo(MemoryDTO memoryDTO) {
        MemoryDTO newMemoryDTO = new MemoryDTO();
        newMemoryDTO.setMessageId(memoryDTO.getMessageId());
        newMemoryDTO.setMessageContent(memoryDTO.getMessageContent());
        newMemoryDTO.setMessageCreatorName(memoryDTO.getMessageCreatorName());
        newMemoryDTO.setMessageCreateAt(memoryDTO.getMessageCreateAt());
        newMemoryDTO.setMessageImportanceScore(memoryDTO.getMessageImportanceScore());
        return JSON.toJSONString(newMemoryDTO);
    }


    private static void checkAndInsertDepthLeafReflection(Integer leafDepth, String depthLeafKey, String depthLeafListKey, String ownerId, String ownerName, String ownerType) {
        // 总token提炼限制
        if (RedisLikeCounter.get(depthLeafKey) < REFLECTION_TOKEN_LIMIT) {
            return;
        }
        List<MemoryReflection.ReflectionResult.Reflection> reflectionList = MemoryReflection.extractReflectionFromMessages(RedisLikeCounter.getList(depthLeafListKey));
        RedisLikeCounter.reset(depthLeafKey);
        RedisLikeCounter.reset(depthLeafListKey);
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
                            .messageParentIds(parentIdList).messageContent(reflectionText).aiResponseFlag(NO_STR)
                            .messageCreateAt(new Date()).messageReceiveId(ownerId).messageReceiveName(ownerName).messageReceiveType(ownerType)
                            .messageOwnerId(ownerId).messageOwnerName(ownerName).messageOwnerType(ownerType)
                            .memoryLeafDepth(leafDepth + 1).useToken(OpenAiTokenizerUtil.getMessageToken(new UserMessage(reflectionText))).build();
            insertNewMemory(memoryDTO);
        }
    }

}
