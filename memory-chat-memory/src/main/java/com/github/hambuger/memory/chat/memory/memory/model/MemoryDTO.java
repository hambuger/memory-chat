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
     * message id
     */
    private String messageId;


    /**
     * information depth
     */
    private Integer memoryLeafDepth;


    /**
     * The importance of the news
     */
    private Double messageImportanceScore;

    /**
     * Message last read time
     */
    private String messageLastAccessTime;


    /**
     * message vector
     */
    private List<Double> messageContentVector;

    /**
     * Emotion
     */
    private String emotion;

    /**
     * summary words
     */
    private List<String> summaryWords;


    /**
     * Number of tokens consumed
     */
    private Integer useToken;

    /**
     * message parent id
     */
    private List<String> messageParentIds;

    /**
     * WHETHER AI REPLIES 1 YES 0 NO
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
