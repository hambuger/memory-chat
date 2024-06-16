package memory;

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

import elasticsearch.EsClient;
import embeddings.TextEmbeddings;
import memory.model.MemoryDTO;
import util.IdUtil;
import util.OpenAiTokenizerUtil;
import util.RedisLikeCounter;
import wechat.ChatCompletionsApi;

import static constants.Constants.AI_CREATOR_ID;
import static constants.Constants.AI_CREATOR_NAME;
import static constants.Constants.CHAT_MEMORY_INDEX;
import static constants.Constants.REFLECTION_TOKEN_LIMIT;


/**
 * @author hamburger
 * @since 2024/6/13
 */
public class MemoryInsert {


    public static Boolean insertNewMemory(MemoryDTO memoryDTO) {
        if (StringUtils.isBlank(memoryDTO.getMessageId())) {
            memoryDTO.setMessageId(IdUtil.generateUniqueId());
        }
        String msgListKey = memoryDTO.getMessageOwnerId() + "::" + (Objects.equal(memoryDTO.getAiResponseFlag(), "0") ? memoryDTO.getMessageCreatorId() : memoryDTO.getMessageReceiveId()) + "::msg";
        RedisLikeCounter.addMsg(msgListKey, MemoryDTO.builder().messageId(memoryDTO.getMessageId()).messageContentType(memoryDTO.getMessageContentType()).aiResponseFlag(memoryDTO.getAiResponseFlag()).messageContent(memoryDTO.getMessageContent()).build());
        boolean textMsgFlag = StringUtils.equals(memoryDTO.getMessageContentType(), "TEXT");
        boolean userMsgFlag = StringUtils.equals(memoryDTO.getAiResponseFlag(), "0");
        // 生成重要性分数
        String messageContent = memoryDTO.getMessageContent();
        if (textMsgFlag) {
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
            String depthLeafCountKey = memoryDTO.getMessageOwnerId() + "::" + memoryDTO.getMemoryLeafDepth();
            String depthLeafListKey = memoryDTO.getMessageOwnerId() + "::list::" + memoryDTO.getMemoryLeafDepth();
            RedisLikeCounter.incrBy(depthLeafCountKey, memoryDTO.getUseToken());
            String jsonInfo = getAiUseJsonInfo(memoryDTO);
            RedisLikeCounter.addElement(depthLeafListKey, jsonInfo);
            checkAndInsertDepthLeafReflection(memoryDTO.getMemoryLeafDepth(), depthLeafCountKey, depthLeafListKey, memoryDTO.getMessageOwnerId(), memoryDTO.getMessageOwnerName(), memoryDTO.getMessageOwnerType());
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
            MemoryDTO memoryDTO = MemoryDTO.builder().messageCreatorId(AI_CREATOR_ID).messageCreatorName(AI_CREATOR_NAME).messageContentType("1").messageParentIds(parentIdList).messageContent(reflectionText).aiResponseFlag("0").messageCreateAt(new Date()).messageContentType("REFLECTION").messageReceiveId(ownerId).messageOwnerId(ownerId).messageReceiveName(ownerName).messageReceiveName(ownerName).messageOwnerType(ownerType).messageReceiveType(ownerType).memoryLeafDepth(leafDepth + 1).useToken(OpenAiTokenizerUtil.getTextToken(reflectionText)).build();
            insertNewMemory(memoryDTO);
        }
    }

}
