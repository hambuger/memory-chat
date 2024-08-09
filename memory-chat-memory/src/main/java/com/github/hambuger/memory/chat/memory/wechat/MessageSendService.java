package com.github.hambuger.memory.chat.memory.wechat;

import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.chat.model.ContentTypeEnum;
import com.github.hambuger.memory.chat.memory.chat.model.CreatorEnum;
import com.github.hambuger.memory.chat.memory.other.functionCall.aop.FunctionCallRegistry;
import com.github.hambuger.memory.chat.memory.memory.create.MemoryInsert;
import com.github.hambuger.memory.chat.memory.memory.model.MemoryDTO;
import com.github.hambuger.memory.chat.wechat.api.MessageTools;
import com.github.hambuger.memory.chat.wechat.entity.Message;
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


    @FunctionCallRegistry(functionDesc = "处理回复消息,所有对用户的回复都应该使用这个方法。而且如果不需要回复处理，也可以通过这个方法告知。如果要回复多条信息，更应该使用这个方法", scene = {ChatSceneEnum.NORMAL_USER, ChatSceneEnum.NORMAL_GROUP, ChatSceneEnum.SCHEDULE, ChatSceneEnum.NEWS_SCHEDULE})
    public boolean replyMessageProcessing(SendMessageRequest sendMessageRequest) {
        return true;
    }

    @FunctionCallRegistry(functionDesc = "给某人发送消息", scene = {ChatSceneEnum.TASK})
    public boolean sendToOthersMessage(SendOthersMessageRequest sendMessageRequest) {
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
        Message message = new Message();
        message.setToRemarkname(sendMessageRequest.getReceiveName());
        message.setContent(dto.getMessageContent());
        message.setMsgType(ContentTypeEnum.TEXT.getMsgType());
        MessageTools.sendMsgByRemarkName(message);
        return true;
    }

}
