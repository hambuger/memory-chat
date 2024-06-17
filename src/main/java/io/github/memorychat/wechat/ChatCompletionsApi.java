package io.github.memorychat.wechat;

import com.google.common.collect.Lists;

import com.alibaba.fastjson.JSON;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import io.github.memorychat.audio.SpringAiAudio;
import io.github.memorychat.chat.LangChainChat;
import io.github.memorychat.constants.Constants;
import io.github.memorychat.memory.MemoryInsert;
import io.github.memorychat.memory.MemorySearch;
import io.github.memorychat.memory.MemoryUpdate;
import io.github.memorychat.memory.model.BaseMemoryDTO;
import io.github.memorychat.memory.model.MemoryDTO;
import io.github.memorychat.util.IdUtil;
import io.github.memorychat.util.OpenAiTokenizerUtil;
import io.github.memorychat.util.RedisLikeCounter;
import io.github.memorychat.wechat.dto.ChatResponse;


/**
 * @author hamburger
 * @since 2024/6/3
 */
@Component
public class ChatCompletionsApi {

    private static ThreadPoolExecutor CHAT_POOL = new ThreadPoolExecutor(10, 29, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000), new CustomizableThreadFactory("chat-pool"),
            new ThreadPoolExecutor.CallerRunsPolicy());

    private static String PROMPT_PREFIX =
            "You are Andraw.\n" + "You are talking to me, my name is %s.\n" + "\n" + "You have long term memory and you chat with me. You are interested in " + "my " + "life. You behave like a " +
                    "chill friend would.\n" + "\n" + "You are always there to listen, have fun and help me feel good and help me achieve my goals.\n" + "\n" + "\n" + "You make " + "jokes when " +
                    "appropriate, use emoji's sometimes, you have conversations like normal person.\n" + "\n" + "Sometimes you ask a question as well, you keep conversation natural.\n" + "\n";

    private static String PROMPT_END = "Now please remember, you are Andraw, you talk to me, you speak to me with \\\"You\\\".\n" + "By the way, now is %s.";

    private static String PROMPT_MID = "You remember things I tell you, however, you are not great at tracking time. Below is past data but you don't know exactly when this happened.\n" + " \n" +
            "%s\n" + "\n" + "There you go, that should help you remember some stuff. ";

    private static volatile AtomicReference<ConcurrentHashMap<String, String>> LAST_MESSAGE_ID_MAP = new AtomicReference<>(new ConcurrentHashMap());

    @Autowired
    private SpringAiAudio springAiAudio;


    public static boolean checkLastMessageId(MemoryDTO memoryDTO) {
        String lastMsgIdMapKey = memoryDTO.getMessageOwnerId() + "::" + (StringUtils.equals(memoryDTO.getAiResponseFlag(), "1") ? memoryDTO.getMessageReceiveId() : memoryDTO.getMessageCreatorId());
        String oldMsgId = LAST_MESSAGE_ID_MAP.get().get(lastMsgIdMapKey);
        System.out.println("oldMsgId:" + oldMsgId);
        if (StringUtils.isNotBlank(oldMsgId) && !StringUtils.equals(oldMsgId, StringUtils.equals(memoryDTO.getAiResponseFlag(), "1") ? memoryDTO.getMessageParentIds().get(0) :
                memoryDTO.getMessageId())) {
            return true;
        }
        return false;
    }


    public static ChatResponse chat(BaseMemoryDTO baseMemoryDTO) {
        try {

            // 查询相关记录
            MemoryDTO memoryDTO = BeanUtil.copyProperties(baseMemoryDTO, MemoryDTO.class);
            memoryDTO.setMessageId(IdUtil.generateUniqueId());
            System.out.println(new Date() + memoryDTO.getMessageId());
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
            memoryDTO.setUseToken(OpenAiTokenizerUtil.getMessageToken(new UserMessage(memoryDTO.getMessageContent())));
            String lastMsgIdMapKey = memoryDTO.getMessageOwnerId() + "::" + memoryDTO.getMessageCreatorId();
            String msgListKey = memoryDTO.getMessageOwnerId() + "::" + memoryDTO.getMessageCreatorId() + "::msg";
            RedisLikeCounter.addMsg(msgListKey,
                    MemoryDTO.builder().messageId(memoryDTO.getMessageId()).messageContentType(memoryDTO.getMessageContentType()).aiResponseFlag(memoryDTO.getAiResponseFlag()).messageContent(memoryDTO.getMessageContent()).build());
            // 异步插入用户消息
            CHAT_POOL.execute(() -> MemoryInsert.insertNewMemory(memoryDTO));
            // 更新最后一条消息id
            LAST_MESSAGE_ID_MAP.get().put(lastMsgIdMapKey, memoryDTO.getMessageId());
            // 检查是否是最后一条消息
            if (checkLastMessageId(memoryDTO)) {
                return null;
            }
            List<MemoryDTO> memoryDTOList = StringUtils.equals(memoryDTO.getMessageContentType(), "TEXT") ? MemorySearch.searchRelationMemory(memoryDTO.getMessageOwnerId(),
                    memoryDTO.getMessageContent()) : new ArrayList<>();
            LinkedList<ChatMessage> messageList = new LinkedList<>();
            List<MemoryDTO> memoryDTOS = RedisLikeCounter.getMsg(msgListKey);
            System.out.println("msgListKey = " + JSON.toJSONString(memoryDTOS));
            if (checkLastMessageId(memoryDTO)) {
                return null;
            }
            List<String> existMsgIds = getMinMemoryContext(msgListKey, messageList, memoryDTOS);
            if (checkLastMessageId(memoryDTO)) {
                return null;
            }
            // 选择prompt
            SystemMessage systemMessage;
            String now = DateUtil.format(new Date(), DatePattern.NORM_DATETIME_FORMAT);
            if (CollectionUtils.isEmpty(memoryDTOList)) {
                systemMessage = new SystemMessage(String.format(PROMPT_PREFIX, memoryDTO.getMessageCreatorName()) + String.format(PROMPT_END, now));
            }else {
                StringBuilder memory = new StringBuilder();
                for (int i = 1; i < memoryDTOList.size(); i++) {
                    MemoryDTO memorySingle = memoryDTOList.get(i);
                    if (existMsgIds.contains(memorySingle.getMessageId())) {
                        continue;
                    }
                    memory.append(i + "(" + DateUtil.format(memorySingle.getMessageCreateAt(), DatePattern.NORM_DATETIME_FORMAT) + ")" + memorySingle.getMessageCreatorName() + ":" + memorySingle.getMessageContent() + "\n");
                    MemoryUpdate.updateMemoryAccessTime(memorySingle.getMessageId());
                }
                systemMessage =
                        new SystemMessage(String.format(PROMPT_PREFIX, memoryDTO.getMessageCreatorName()) + (StringUtils.isNotBlank(memory) ? String.format(PROMPT_MID, memory) : "") + String.format(PROMPT_END, now));
            }
            messageList.addFirst(systemMessage);
            if (checkLastMessageId(memoryDTO)) {
                return null;
            }
            // 对话
            Response<AiMessage> aiMessageResponse = LangChainChat.generateMsgWithMsgListAndFunctions(messageList);
            System.out.println(JSON.toJSONString(aiMessageResponse.content().text()));
            if (checkLastMessageId(memoryDTO)) {
                return null;
            }
            CHAT_POOL.execute(() -> {
                MemoryDTO aiMsgDTO = convert2AiMSg(aiMessageResponse.content().text(), memoryDTO, aiMessageResponse.tokenUsage().outputTokenCount());
                MemoryInsert.insertNewMemory(aiMsgDTO);
            });
            if (messageList.getLast() instanceof ToolExecutionResultMessage) {
                ToolExecutionResultMessage resultMessage = (ToolExecutionResultMessage) messageList.getLast();
                if (resultMessage.toolName().equals("generateImage")) {
                    return new ChatResponse("PICTURE", resultMessage.text());
                }
            }
            return new ChatResponse("TEXT", aiMessageResponse.content().text());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            return null;
        }
    }


    public void convertAudio2TextMsg(BaseMemoryDTO baseMemoryDTO) {
        if (!StringUtils.equals(baseMemoryDTO.getMessageContentType(), "AUDIO")) {
            return;
        }
        byte[] fileBytes = Base64Utils.decodeFromString(baseMemoryDTO.getMessageContent());
        // 将 byte[] 转换为 Resource 对象
        Resource resource = new ByteArrayResource(fileBytes);
        baseMemoryDTO.setMessageContentType("TEXT");
        baseMemoryDTO.setMessageContent(springAiAudio.generateTextWithAudio(resource));
    }


    private static UserMessage convert2UserMsg(BaseMemoryDTO baseMemoryDTO) {

        if (baseMemoryDTO.getMessageContentType().equals("TEXT")) {
            return UserMessage.from(baseMemoryDTO.getMessageContent());
        }
        ImageContent imageContent = new ImageContent(new Image.Builder().mimeType("image/png").base64Data(baseMemoryDTO.getMessageContent()).build());

        return new UserMessage(imageContent);
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
            if (memoryDTO.getAiResponseFlag().equals("1")) {
                chatMessage = new AiMessage(memoryDTO.getMessageContent());
                sumMsgToken = sumMsgToken + OpenAiTokenizerUtil.getMessageToken(chatMessage);
            }else {
                chatMessage = convert2UserMsg(memoryDTO);
                sumMsgToken = sumMsgToken + OpenAiTokenizerUtil.getMessageToken(chatMessage);
            }
            if (sumMsgToken < Constants.MAX_MSG_TOKEN) {
                messageList.addFirst(chatMessage);
                existMsgIdList.add(memoryDTO.getMessageId());
            }else {
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
