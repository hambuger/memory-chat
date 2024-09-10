package com.github.hambuger.memory.chat.memory.chat;

import com.github.hambuger.memory.chat.memory.chat.message.SendMessageRequest;
import com.github.hambuger.memory.chat.memory.chat.message.SendOthersMessageRequest;
import com.github.hambuger.memory.chat.memory.chat.model.*;
import com.github.hambuger.memory.chat.memory.other.functionCall.aop.FunctionCallRegistry;
import com.github.hambuger.memory.chat.memory.memory.create.MemoryInsert;
import com.github.hambuger.memory.chat.memory.memory.model.MemoryDTO;
import com.github.hambuger.memory.chat.memory.other.util.RedisUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import static com.github.hambuger.memory.chat.memory.chat.ChatCompletionsApi.getAiResponseMemoryDTO;
import static com.github.hambuger.memory.chat.memory.other.constants.CommonConstants.NO_STR;

@Slf4j
@Component
public class MessageSendService {

    @Resource
    private MemoryInsert memoryInsert;

    @Resource
    private CommonMessageHandler commonMessageHandler;

    @Resource
    private RedisUtil redisUtil;


    @FunctionCallRegistry(functionDesc = "Reply to a message or send a new message. All replies should use this method. If you do not need to reply to a message or send a new message, also inform through this method. You can use this method to send multiple messages.", scene = {ChatSceneEnum.NORMAL_USER, ChatSceneEnum.NORMAL_GROUP, ChatSceneEnum.SCHEDULE, ChatSceneEnum.NEWS_SCHEDULE})
    public Boolean replyOrStartNewMessage(SendMessageRequest sendMessageRequest) {
        return true;
    }

    @FunctionCallRegistry(functionDesc = "Send a message to someone", scene = {ChatSceneEnum.TASK})
    public Boolean sendMessageToOthers(SendOthersMessageRequest sendMessageRequest) {
        MemoryDTO memoryDTO = new MemoryDTO();
        memoryDTO.setMessageCreatorId(sendMessageRequest.getReceiveName());
        memoryDTO.setMessageCreatorName(sendMessageRequest.getReceiveName());
        memoryDTO.setMessageCreatorType(CreatorEnum.USER.getType());
        memoryDTO.setMessageOwnerId(CreatorEnum.Andrew.getUserId());
        memoryDTO.setMessageOwnerName(CreatorEnum.Andrew.getUserName());
        memoryDTO.setMessageOwnerType(CreatorEnum.Andrew.getType());
        memoryDTO.setGroupMsgFlag(NO_STR);
        MemoryDTO dto = getAiResponseMemoryDTO(memoryDTO, ContentTypeEnum.TEXT.getType(), StringUtils.join(sendMessageRequest.getSendTextMessageList(), "。"), 0);
        memoryInsert.insertNewMemory(dto, true);
        SendChannelMessageRequest message = new SendChannelMessageRequest();
        ChatMember member = redisUtil.getMember(sendMessageRequest.getReceiveName());
        message.setToUserId(member.getSendUserId());
        message.setMessageContent(dto.getMessageContent());
        message.setMessageContentType(ContentTypeEnum.TEXT.getType());
        message.setChannelEnum(member.getChannelScene());
        commonMessageHandler.sendMessage(message);
        return true;
    }

}
