package com.github.hambuger.memory.chat.memory.chat;

import com.github.hambuger.memory.chat.memory.chat.model.*;
import com.github.hambuger.memory.chat.memory.emoji.SogouEmoji;
import com.github.hambuger.memory.chat.memory.other.util.FileUtil;
import com.github.hambuger.memory.chat.memory.other.util.RedisUtil;
import com.github.hambuger.memory.chat.memory.chat.message.SendMessage;
import com.github.hambuger.memory.chat.memory.other.util.UserInfoUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@Component
public class CommonMessageHandler implements MessageHandler {

    @Resource
    private ChatCompletionsApi chatCompletionsApi;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private SogouEmoji sogouEmoji;

    @Resource
    private FileUtil fileUtil;

    private static final Map<String, Consumer<SendChannelMessageRequest>> SEND_TOOL_MAP = new ConcurrentHashMap<>();

    public void registerSendTool(String channelEnum, Consumer<SendChannelMessageRequest> consumer) {
        SEND_TOOL_MAP.put(channelEnum, consumer);
    }


    @Override
    public ChatResponse receiveNewMsg(BaseReceiveMessage msg) {
        long receiveMsgTime = System.currentTimeMillis();
        ChatResponse response = chatCompletionsApi.chat(msg);
        if (response == null || CollectionUtils.isEmpty(response.getSendMessageList())) {
            return null;
        }
        if (SEND_TOOL_MAP.get(msg.getChannelEnum()) == null) {
            return response;
        }
        for (SendMessage sendMessage : response.getSendMessageList()) {
            SendChannelMessageRequest message = new SendChannelMessageRequest();
            message.setToUserId(msg.getReceiveMessageUserId());
            message.setMessageContent(sendMessage.getMessageContent());
            ContentTypeEnum contentTypeEnum = ContentTypeEnum.getByType(sendMessage.getMessageContentType());
            message.setMessageContentType(sendMessage.getMessageContentType());
            if (contentTypeEnum == ContentTypeEnum.PICTURE) {
                String filePath = fileUtil.downloadImage(sendMessage.getMessageContent());
                message.setFilePath(filePath);
                message.setMessageContent(null);
            }else if (contentTypeEnum == ContentTypeEnum.EMOJI) {
                String emojiPath = sogouEmoji.downloadImage(sendMessage.getMessageContent());
                if (StringUtils.isBlank(emojiPath)) {
                    message.setMessageContentType(ContentTypeEnum.TEXT.getType());
                }else {
                    message.setFilePath(emojiPath);
                    if (!emojiPath.endsWith("gif")) {
                        message.setMessageContentType(ContentTypeEnum.PICTURE.getType());
                    }
                }
                message.setMessageContent(null);
            }else if (contentTypeEnum == ContentTypeEnum.TEXT) {
                int waste = message.getMessageContent().length() * 1000 / 4;
                if (System.currentTimeMillis() < receiveMsgTime + waste) {
                    try {
                        Thread.sleep(receiveMsgTime + waste - System.currentTimeMillis());
                    } catch (InterruptedException e) {
                        log.warn("sleep error", e);
                    }
                }
            }
            message.setChannelEnum(msg.getChannelEnum());
            this.sendMessage(message);
            receiveMsgTime = System.currentTimeMillis();
        }
        return response;
    }

    @Override
    public void sendMessage(SendChannelMessageRequest request) {
        String channelName = null;
        if (StringUtils.isNotBlank(request.getChannelEnum())) {
            channelName = request.getChannelEnum();
        } else if (UserInfoUtil.getUser() != null) {
            ChatMember member = redisUtil.getMember(UserInfoUtil.getUser());
            if (member != null) {
                channelName = member.getChannelScene();
            }
        }
        if (StringUtils.isBlank(channelName)) {
            channelName = MessageChannelEnum.WECHAT.name();
        }
        SEND_TOOL_MAP.get(channelName).accept(request);
    }
}
