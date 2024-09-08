package com.github.hambuger.memory.chat.wechat.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.hambuger.memory.chat.memory.chat.CommonMessageHandler;
import com.github.hambuger.memory.chat.memory.chat.model.BaseReceiveMessage;
import com.github.hambuger.memory.chat.memory.chat.model.ContentTypeEnum;
import com.github.hambuger.memory.chat.memory.chat.model.MessageChannelEnum;
import com.github.hambuger.memory.chat.memory.chat.model.SendChannelMessageRequest;
import com.github.hambuger.memory.chat.memory.other.constants.CommonConstants;
import com.github.hambuger.memory.chat.wechat.api.ContactsTools;
import com.github.hambuger.memory.chat.wechat.api.DownloadTools;
import com.github.hambuger.memory.chat.wechat.api.MessageTools;
import com.github.hambuger.memory.chat.wechat.constant.WxReqParamsConstant;
import com.github.hambuger.memory.chat.wechat.constant.WxRespConstant;
import com.github.hambuger.memory.chat.wechat.entity.Message;
import com.github.hambuger.memory.chat.wechat.service.IMsgHandlerFace;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;


@Log4j2
@Component
public class IMsgHandlerFaceImpl implements IMsgHandlerFace {

    @Resource
    private CommonMessageHandler commonMessageHandler;

    public void sendMessage(SendChannelMessageRequest request) {
        Message message = new Message();
        message.setToUsername(request.getToUserId());
        message.setContent(request.getMessageContent());
        ContentTypeEnum contentTypeEnum = ContentTypeEnum.getByType(request.getMessageContentType());
        message.setMsgType(contentTypeEnum == null ? ContentTypeEnum.TEXT.getMsgType() : contentTypeEnum.getMsgType());
        message.setFilePath(request.getFilePath());
        MessageTools.sendMsgByUserId(message);
    }

    @PostConstruct
    public void init(){
        commonMessageHandler.registerSendTool(MessageChannelEnum.WECHAT.name(), this::sendMessage);
    }


    @Override
    public List<Message> textMsgHandle(Message msg) {
        return dealNewMsg(msg);

    }


    @Nullable
    private List<Message> dealNewMsg(Message msg) {
        if (msg.getIsSend()) {
            return null;
        }
        BaseReceiveMessage baseMemoryDTO = new BaseReceiveMessage();
        baseMemoryDTO.setMessageContent(msg.getContent());
        ContentTypeEnum sendMsgContentTypeEnum = ContentTypeEnum.getByWxType(msg.getMsgType());
        if (sendMsgContentTypeEnum == null) {
            // 不支持类型处理
            return null;
        }
        baseMemoryDTO.setMessageContentType(sendMsgContentTypeEnum.getType());
        // 对于语音和图片,表情，特殊处理文件路径
        if (sendMsgContentTypeEnum == ContentTypeEnum.APP || sendMsgContentTypeEnum == ContentTypeEnum.VIDEO || sendMsgContentTypeEnum == ContentTypeEnum.AUDIO || sendMsgContentTypeEnum == ContentTypeEnum.PICTURE || sendMsgContentTypeEnum == ContentTypeEnum.EMOJI) {
            baseMemoryDTO.setMessageContent(msg.getFilePath());
        }
        baseMemoryDTO.setMessageCreatorName(StringUtils.isNoneBlank(msg.getFromRemarkname()) ? msg.getFromRemarkname() : msg.getFromNickname());
        baseMemoryDTO.setGroupMsgFlag(msg.isGroup() ? CommonConstants.YES_STR : CommonConstants.NO_STR);
        baseMemoryDTO.setRealCreatorId(StringUtils.isNoneBlank(msg.getFromMemberOfGroupNickname()) ? msg.getFromMemberOfGroupNickname() : msg.getFromMemberOfGroupDisplayname());
        baseMemoryDTO.setRealCreatorName(baseMemoryDTO.getRealCreatorId());
        baseMemoryDTO.setChannelEnum(MessageChannelEnum.WECHAT.name());
        baseMemoryDTO.setReceiveMessageUserId(msg.getFromUsername());
        commonMessageHandler.receiveNewMsg(baseMemoryDTO);
        return null;
    }


    /**
     * 图片消息(non-Javadoc)
     *
     * @see
     */
    @Override
    public List<Message> picMsgHandle(Message msg) {
        DownloadTools.awaitDownload(msg.getFilePath());
        return dealNewMsg(msg);
    }


    /**
     * 语音消息(non-Javadoc)
     *
     * @see
     */
    @Override
    public List<Message> voiceMsgHandle(Message msg) {
        DownloadTools.awaitDownload(msg.getFilePath());
        return dealNewMsg(msg);
    }


    @Override
    public List<Message> videoMsgHandle(Message msg) {
        DownloadTools.awaitDownload(msg.getFilePath());
        return dealNewMsg(msg);
    }


    @Override
    public List<Message> undoMsgHandle(Message msg) {
        return null;
    }


    @Override
    public List<Message> addFriendMsgHandle(Message msg) {
        String nickName = "";
        if (StringUtils.isNotBlank(msg.getMsgJson())) {
            JSONObject msgJson = JSONObject.parseObject(msg.getMsgJson());
            nickName = Optional.ofNullable(msgJson.getJSONObject("recommendInfo")).map(obj -> obj.getString("content")).map(content -> content.replace("我是", "")).orElse("");
        }
        MessageTools.addFriend(msg.getContactsUserName(), msg.getContactsTicket());
        try {
            MessageTools.modifyRemarkName(msg.getContactsUserName(), nickName);
            ContactsTools.getContactByUserName(msg.getContactsUserName()).setRemarkname(nickName);
        } catch (IOException e) {
            log.error("addFriendMsgHandle error", e);
        }
        return null;
    }


    @Override
    public List<Message> systemMsgHandle(Message msg) {
        return null;
    }


    @Override
    public List<Message> emotionMsgHandle(Message msg) {
        return dealNewMsg(msg);
    }


    @Override
    public List<Message> appMsgHandle(Message msg) {
        switch (WxRespConstant.WXReceiveMsgCodeOfAppEnum.getByCode(msg.getAppMsgType())) {
            case OTHER:
                break;
            case LINK:
                msg.setMsgType(WxReqParamsConstant.WXSendMsgCodeEnum.TEXT.getCode());
                // [链接文本](链接地址)
                msg.setContent(String.format("[%s](%s)", msg.getFileName(), msg.getUrl()));
                dealNewMsg(msg);
                break;
            case FILE:
                DownloadTools.awaitDownload(msg.getFilePath());
                dealNewMsg(msg);
                break;
            case PROGRAM:
                break;
        }
        return null;
    }


    @Override
    public List<Message> verifyAddFriendMsgHandle(Message msg) {
        return null;
    }


    @Override
    public List<Message> mapMsgHandle(Message msg) {
        return null;
    }


    @Override
    public List<Message> nameCardMsgHandle(Message msg) {

        return null;
    }

}
