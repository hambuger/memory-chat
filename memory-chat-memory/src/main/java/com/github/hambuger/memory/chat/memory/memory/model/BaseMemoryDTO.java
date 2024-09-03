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
     * 消息创建者id
     */
    private String messageCreatorId;

    /**
     * 消息创建者名称
     */
    private String messageCreatorName;

    /**
     * 消息创建者类型
     */
    private String messageCreatorType;

    /**
     * 消息接受者id
     */
    private String messageReceiveId;

    /**
     * 消息接受者名称
     */
    private String messageReceiveName;

    /**
     * 消息接受者类型
     */
    private String messageReceiveType;

    /**
     * 消息拥有者id
     */
    private String messageOwnerId;

    /**
     * 消息拥有者名称
     */
    private String messageOwnerName;

    /**
     * 消息拥有者类型
     */
    private String messageOwnerType;

    /**
     * 消息发送时间
     */
    private String messageCreateAt;

    /**
     * 消息内容类型
     */
    private String messageContentType;


    /**
     * 消息内容
     */
    private String messageContent;


    /**
     * 是否群消息，1:是 0:否
     */
    private String groupMsgFlag;

    /**
     * 真正的消息发送者id
     */
    private String realCreatorId;

    /**
     * 真正的消息发送者名称
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
