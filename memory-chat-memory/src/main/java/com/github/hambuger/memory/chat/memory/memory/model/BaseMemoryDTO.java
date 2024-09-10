package com.github.hambuger.memory.chat.memory.memory.model;

import org.apache.commons.lang3.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.github.hambuger.memory.chat.memory.other.constants.CommonConstants.YES_STR;
import static com.github.hambuger.memory.chat.memory.other.constants.MemoryChatConstants.CHAT_LOCK_KEY;
import static com.github.hambuger.memory.chat.memory.other.constants.MemoryChatConstants.LAST_MSG_ID_KEY;


/**
 * @author hamburger
 * @since 2024/6/14
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class BaseMemoryDTO {

    /**
     * Message creator id
     */
    private String messageCreatorId;

    /**
     * Message creator name
     */
    private String messageCreatorName;

    /**
     * Message creator type
     */
    private String messageCreatorType;

    /**
     * message receiver id
     */
    private String messageReceiveId;

    /**
     * Message recipient name
     */
    private String messageReceiveName;

    /**
     * message receiver type
     */
    private String messageReceiveType;

    /**
     * message owner id
     */
    private String messageOwnerId;

    /**
     * message owner name
     */
    private String messageOwnerName;

    /**
     * message owner type
     */
    private String messageOwnerType;

    /**
     * message sending time
     */
    private String messageCreateAt;

    /**
     * message content type
     */
    private String messageContentType;


    /**
     * message content
     */
    private String messageContent;


    /**
     * Whether to group message, 1: yes 0: no
     */
    private String groupMsgFlag;

    /**
     * real message sender id
     */
    private String realCreatorId;

    /**
     * real message sender name
     */
    private String realCreatorName;


    public boolean groupFlag() {
        return StringUtils.equals(this.getGroupMsgFlag(), YES_STR);
    }

    public String chatLockKey() {
        return String.format(CHAT_LOCK_KEY, this.getMessageCreatorName());
    }

    public String lastMsgIdMapKey() {
        return String.format(LAST_MSG_ID_KEY, this.getMessageCreatorName());
    }

}
