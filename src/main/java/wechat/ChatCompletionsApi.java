package wechat;

import com.google.common.collect.Lists;

import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import chat.LangChainChat;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import constants.Constants;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import memory.MemoryInsert;
import memory.MemorySearch;
import memory.MemoryUpdate;
import memory.model.BaseMemoryDTO;
import memory.model.MemoryDTO;
import util.RedisLikeCounter;


/**
 * @author hamburger
 * @since 2024/6/3
 */
public class ChatCompletionsApi {

    private static String PROMPT_PREFIX =
            "You are Andraw.\n" + "You are talking to me, my name is %s.\n" + "\n" + "You have long term memory and you chat with me. You are interested in " + "my " + "life. You behave like a " +
                    "chill friend would.\n" + "\n" + "You are always there to listen, have fun and help me feel good and help me achieve my goals.\n" + "\n" + "\n" + "You make " + "jokes when " +
                    "appropriate, use emoji's sometimes, you have conversations like normal person.\n" + "\n" + "Sometimes you ask a question as well, you keep conversation natural.\n" + "\n";

    private static String PROMPT_END = "Now please remember, you are Andraw, you talk to me, you speak to me with \\\"You\\\".\n" + "By the way, now is %s.";

    private static String PROMPT_MID = "You remember things I tell you, however, you are not great at tracking time. Below is past data but you don't know exactly when this happened.\n" + " \n" +
            "%s\n" + "\n" + "There you go, that should help you remember some stuff. ";


    public static String chat(BaseMemoryDTO baseMemoryDTO) {
        // 查询相关记录
        MemoryDTO memoryDTO = BeanUtil.copyProperties(baseMemoryDTO, MemoryDTO.class);
        memoryDTO.setMessageCreatorId(memoryDTO.getMessageCreatorName());
        memoryDTO.setMessageCreatorType("1");
        memoryDTO.setMessageReceiveId(Constants.AI_CREATOR_ID);
        memoryDTO.setMessageReceiveName(Constants.AI_CREATOR_NAME);
        memoryDTO.setMessageReceiveType("1");
        memoryDTO.setMessageOwnerType("1");
        memoryDTO.setMessageOwnerId(Constants.AI_CREATOR_ID);
        memoryDTO.setMessageOwnerName(Constants.AI_CREATOR_NAME);
        memoryDTO.setMessageCreateAt(new Date());
        memoryDTO.setAiResponseFlag("0");
        memoryDTO.setMemoryLeafDepth(0);
        memoryDTO.setMessageLastAccessTime(new Date());
        memoryDTO.setMessageParentIds(Lists.newArrayList("0"));
        List<MemoryDTO> memoryDTOList = MemorySearch.searchRelationMemory(memoryDTO.getMessageOwnerId(), memoryDTO.getMessageContent());
        LinkedList<ChatMessage> messageList = new LinkedList<>();
        String msgListKey = memoryDTO.getMessageOwnerId() + "::" + memoryDTO.getMessageCreatorId() + "::msg";
        List<MemoryDTO> memoryDTOS = RedisLikeCounter.getMsg(msgListKey);
        List<String> existMsgIds = getMinMemoryContext(msgListKey, messageList, memoryDTOS);
        // 选择prompt
        SystemMessage systemMessage;
        String now = DateUtil.format(new Date(), DatePattern.NORM_DATETIME_FORMAT);
        if (CollectionUtils.isEmpty(memoryDTOList)) {
            systemMessage = new SystemMessage(String.format(PROMPT_PREFIX, memoryDTO.getMessageOwnerName()) + String.format(PROMPT_END, now));
        }else {
            StringBuilder memory = new StringBuilder();
            for (int i = 1; i < memoryDTOList.size(); i++) {
                MemoryDTO memorySingle = memoryDTOList.get(i);
                if (existMsgIds.contains(memorySingle.getMessageId())) {
                    continue;
                }
                memory.append(i + "(" + DateUtil.format(memorySingle.getMessageCreateAt(), DatePattern.NORM_DATETIME_FORMAT) + ")" + memoryDTO.getMessageCreatorName() + ":" + memoryDTO.getMessageContent() + "\n");
                MemoryUpdate.updateMemoryAccessTime(memorySingle.getMessageId());
            }
            systemMessage = new SystemMessage(String.format(PROMPT_PREFIX, memoryDTO.getMessageOwnerName()) + String.format(PROMPT_MID, memory.toString()) + String.format(PROMPT_END, now));
        }
        messageList.addFirst(systemMessage);
        UserMessage userMessage = new UserMessage(baseMemoryDTO.getMessageContent());
        messageList.addLast(userMessage);
        // 对话
        Response<AiMessage> aiMessageResponse = LangChainChat.generateMsgWithMsgList(messageList);
        memoryDTO.setUseToken(aiMessageResponse.tokenUsage().inputTokenCount());
        MemoryInsert.insertNewMemory(memoryDTO);

        MemoryDTO aiMsgDTO = convert2AiMSg(aiMessageResponse.content().text(), memoryDTO, aiMessageResponse.tokenUsage().outputTokenCount());
        MemoryInsert.insertNewMemory(aiMsgDTO);
        return aiMessageResponse.content().text();
    }


    private static List<String> getMinMemoryContext(String msgListKey, LinkedList<ChatMessage> messageList, List<MemoryDTO> memoryDTOS) {
        if (CollectionUtils.isEmpty(memoryDTOS)) {
            return new ArrayList<>();
        }
        List<String> existMsgIdList = new ArrayList<>();
        Integer sumMsgToken = 0;
        for (int i = memoryDTOS.size() - 1; i >= 0; i--) {
            MemoryDTO memoryDTO = memoryDTOS.get(i);
            ChatMessage chatMessage;
            if (memoryDTO.getAiResponseFlag() == "1") {
                chatMessage = new AiMessage(memoryDTO.getMessageContent());
                sumMsgToken = sumMsgToken + sumMsgToken;
            }else {
                chatMessage = new UserMessage(memoryDTO.getMessageContent());
                sumMsgToken = sumMsgToken + sumMsgToken;
            }
            if (sumMsgToken < Constants.MAX_MSG_TOKEN) {
                messageList.addFirst(chatMessage);
                existMsgIdList.add(memoryDTO.getMessageId());
            }else{
                delOldMessageFromCache(msgListKey, i);
                break;
            }

        }
        return existMsgIdList;
    }


    private static void delOldMessageFromCache(String msgListKey, int i) {
        RedisLikeCounter.delOldMemory(msgListKey, i);
    }


    private static MemoryDTO convert2AiMSg(String response, MemoryDTO memoryDTO, int token) {
        MemoryDTO aiMemoryDTO = new MemoryDTO();
        aiMemoryDTO.setMessageCreatorId(Constants.AI_CREATOR_ID);
        aiMemoryDTO.setMessageCreatorName(Constants.AI_CREATOR_NAME);
        aiMemoryDTO.setMessageCreatorType("1");
        aiMemoryDTO.setMessageReceiveId(memoryDTO.getMessageCreatorId());
        aiMemoryDTO.setMessageReceiveName(memoryDTO.getMessageReceiveName());
        aiMemoryDTO.setMessageReceiveType(memoryDTO.getMessageReceiveType());
        aiMemoryDTO.setMessageOwnerId(memoryDTO.getMessageOwnerId());
        aiMemoryDTO.setMessageOwnerName(memoryDTO.getMessageOwnerName());
        aiMemoryDTO.setMessageOwnerType(memoryDTO.getMessageOwnerType());
        aiMemoryDTO.setMessageCreateAt(new Date());
        aiMemoryDTO.setMessageContentType("TEXT");
        aiMemoryDTO.setMessageContent(response);
        aiMemoryDTO.setMemoryLeafDepth(0);
        aiMemoryDTO.setMessageLastAccessTime(new Date());
        aiMemoryDTO.setUseToken(token);
        aiMemoryDTO.setMessageParentIds(Lists.newArrayList(memoryDTO.getMessageId()));
        aiMemoryDTO.setAiResponseFlag("1");
        return aiMemoryDTO;
    }

}
