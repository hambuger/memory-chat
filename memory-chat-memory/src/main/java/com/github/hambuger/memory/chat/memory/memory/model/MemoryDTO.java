package com.github.hambuger.memory.chat.memory.memory.model;

import com.github.hambuger.memory.chat.memory.chat.model.CreatorEnum;
import com.github.hambuger.memory.chat.memory.other.constants.MemoryChatConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

import static com.github.hambuger.memory.chat.memory.other.constants.CommonConstants.NO_STR;


/**
 * @author hamburger
 * @since 2024/6/14
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class MemoryDTO extends BaseMemoryDTO {

    /**
     * 消息ID
     */
    private String messageId;


    /**
     * 信息深度
     */
    private Integer memoryLeafDepth;


    /**
     * 消息重要分
     */
    private Double messageImportanceScore;

    /**
     * 消息最后读取时间
     */
    private String messageLastAccessTime;


    /**
     * 消息向量
     */
    private List<Double> messageContentVector;

    /**
     * 情感
     */
    private String emotion;

    /**
     * 总结词语
     */
    private List<String> summaryWords;


    /**
     * 消耗token数
     */
    private Integer useToken;

    /**
     * 消息父id
     */
    private List<String> messageParentIds;

    /**
     * 是否AI回复，1:是 0:否
     */
    private String aiResponseFlag;

    private String isDeleted = "0";

    public String msgCacheListKey() {
        boolean userMsgFlag = this.userMsgFlag();
        boolean reflectionFlag = StringUtils.equals(this.getMessageCreatorType(), CreatorEnum.REFLECTION.getType());
        return ((userMsgFlag && !reflectionFlag) ? this.getMessageCreatorId() : this.getMessageReceiveId()) + MemoryChatConstants.MSG_LIST_KEY_SUFFIX;
    }

    public String msgReflectionListKey() {
        boolean userMsgFlag = this.userMsgFlag();
        boolean reflectionFlag = StringUtils.equals(this.getMessageCreatorType(), CreatorEnum.REFLECTION.getType());
        return ((userMsgFlag && !reflectionFlag) ? this.getMessageCreatorId() : this.getMessageReceiveId()) + MemoryChatConstants.DEPTH_LEAF_LIST_KEY_MID + this.getMemoryLeafDepth();
    }

    public String msgReflectionCountKey() {
        boolean userMsgFlag = this.userMsgFlag();
        boolean reflectionFlag = StringUtils.equals(this.getMessageCreatorType(), CreatorEnum.REFLECTION.getType());
        return ((userMsgFlag && !reflectionFlag) ? this.getMessageCreatorId() : this.getMessageReceiveId()) + MemoryChatConstants.DEPTH_LEAF_TOKEN_COUNT_KEY_MID + this.getMemoryLeafDepth();
    }



    public boolean userMsgFlag(){
        return StringUtils.equals(this.getAiResponseFlag(), NO_STR);
    }

}
