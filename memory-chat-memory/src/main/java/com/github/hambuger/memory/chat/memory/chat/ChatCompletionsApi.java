package com.github.hambuger.memory.chat.memory.chat;

import com.google.common.collect.Lists;

import com.alibaba.fastjson.JSON;
import com.github.hambuger.memory.chat.memory.audio.SpringAiAudio;
import com.github.hambuger.memory.chat.memory.chat.dto.ChatResponse;
import com.github.hambuger.memory.chat.memory.chat.dto.ContentTypeEnum;
import com.github.hambuger.memory.chat.memory.chat.dto.CreatorEnum;
import com.github.hambuger.memory.chat.memory.constants.CommonConstants;
import com.github.hambuger.memory.chat.memory.constants.Constants;
import com.github.hambuger.memory.chat.memory.memory.MemoryInsert;
import com.github.hambuger.memory.chat.memory.memory.MemorySearch;
import com.github.hambuger.memory.chat.memory.memory.MemoryUpdate;
import com.github.hambuger.memory.chat.memory.memory.model.BaseMemoryDTO;
import com.github.hambuger.memory.chat.memory.memory.model.MemoryDTO;
import com.github.hambuger.memory.chat.memory.util.IdUtil;
import com.github.hambuger.memory.chat.memory.util.OpenAiTokenizerUtil;
import com.github.hambuger.memory.chat.memory.util.RedisLikeCounter;
import com.github.hambuger.memory.chat.memory.wechat.SendMessageRequest;
import com.github.hambuger.memory.chat.wechat.api.DownloadTools;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.extern.slf4j.Slf4j;

import static com.github.hambuger.memory.chat.memory.constants.CommonConstants.DOUBLE_COLON;
import static com.github.hambuger.memory.chat.memory.constants.CommonConstants.YES_STR;
import static com.github.hambuger.memory.chat.memory.constants.Constants.IMAGE_TYPE;


/**
 * @author hamburger
 * @since 2024/6/3
 */
@Slf4j
@Component
public class ChatCompletionsApi {

    private static final ThreadPoolExecutor CHAT_POOL = new ThreadPoolExecutor(10, 20, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000), new CustomizableThreadFactory("chat-pool"),
            new ThreadPoolExecutor.CallerRunsPolicy());

    private static final String PROMPT_PREFIX = "You are Andraw.\n" + "You are talking to me, my name is %s.\n" + "\n" + "You have long term memory and you chat with me. You are interested in " +
            "my " + "life. You behave like a " + "chill friend would.\n" + "\n" + "You are always there to listen, have fun and help me feel good and help me achieve my goals.\n" + "\n" + "\n" +
            "You make " + "jokes when " + "appropriate, use emoji's sometimes, you have conversations like normal person.\n" + "\n" + "You can ask questions if necessary. Your speech will always " +
            "be" + " colloquial, not formal, and not long-winded.The reply message should not be too long. A long message will make the other party feel pressured. If the reply message is too long, you can reply in multiple messages.\n" + "\n";

    private static final String GROUP_PROMPT_PREFIX = "You are Andraw.\n" + "You are talking in a Wechat Group, the group name is %s.\n" + "\n" + "You have long term memory and you chat with " +
            "others" + ". You are interested in " + "their " + "life. You behave like a " + "chill friend would.\n" + "\n" + "You are always there to listen, have fun and help me feel good and help" +
            " others " + "achieve their goals.\n" + "\n" + "\n" + "You make " + "jokes when " + "appropriate, use emoji's sometimes, you have conversations like normal person.\n" + "\n" + "You can " +
            "ask questions" + " if necessary. Your speech will always be colloquial, not formal, and not long-winded.The reply message should not be too long. A long message will make the other party feel pressured. If the reply message is too long, you can reply in multiple messages.\n" + "\n";
    ;

    private static final String PROMPT_END = "Now please remember, you are Andraw, you talk to me, you speak to me with \\\"You\\\".\n" + "By the way, now is %s.";

    private static final String GROUP_PROMPT_END = "Now please remember, you are Andraw, you talk to others, you speak to others with \\\"You\\\".\n" + "By the way, now is %s.";

    ;

    private static final String PROMPT_MID =
            "You remember things I tell you, however, you are not great at tracking time. Below is past data but you don't know exactly when this happened.\n" + " " + "\n" + "%s\n" + "\n" + "There " +
                    "you go, that should help you remember some stuff. ";

    private static final String GROUP_PROMPT_MID =
            "You remember things what happened before, however, you are not great at tracking time. Below is past data but you don't know exactly when this " + "happened.\n" + " \n" + "%s\n" + "\n" + "There you go, that should help you remember some stuff. ";

    ;

    private static final AtomicReference<ConcurrentHashMap<String, String>> LAST_MESSAGE_ID_MAP = new AtomicReference<>(new ConcurrentHashMap());

    @Autowired
    private SpringAiAudio springAiAudio;

    @Autowired
    private SpringAiChat springAiChat;


    public static boolean checkLastMessageId(MemoryDTO memoryDTO) {
        String lastMsgIdMapKey = memoryDTO.getMessageOwnerId() + DOUBLE_COLON + (StringUtils.equals(memoryDTO.getAiResponseFlag(), YES_STR) ? memoryDTO.getMessageReceiveId() :
                memoryDTO.getMessageCreatorId());
        String oldMsgId = LAST_MESSAGE_ID_MAP.get().get(lastMsgIdMapKey);
        return StringUtils.isNotBlank(oldMsgId) && !StringUtils.equals(oldMsgId, StringUtils.equals(memoryDTO.getAiResponseFlag(), YES_STR) ? memoryDTO.getMessageParentIds().get(0) :
                memoryDTO.getMessageId());
    }


    public ChatResponse chat(BaseMemoryDTO baseMemoryDTO) {
        try {
            // 查询相关记录
            convertAudio2TextMsg(baseMemoryDTO);
            MemoryDTO memoryDTO = BeanUtil.copyProperties(baseMemoryDTO, MemoryDTO.class);
            memoryDTO.setMessageId(IdUtil.generateUniqueId());
            memoryDTO.setMessageCreatorId(memoryDTO.getMessageCreatorName());
            memoryDTO.setMessageCreatorType(StringUtils.equals(YES_STR, baseMemoryDTO.getGroupMsgFlag()) ? CreatorEnum.USER.getType() : CreatorEnum.GROUP.getType());
            memoryDTO.setMessageReceiveId(CreatorEnum.Andrew.getUserId());
            memoryDTO.setMessageReceiveName(CreatorEnum.Andrew.getUserName());
            memoryDTO.setMessageReceiveType(CreatorEnum.Andrew.getType());
            memoryDTO.setMessageOwnerType(CreatorEnum.Andrew.getType());
            memoryDTO.setMessageOwnerId(CreatorEnum.Andrew.getUserId());
            memoryDTO.setMessageOwnerName(CreatorEnum.Andrew.getUserName());
            memoryDTO.setMessageCreateAt(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_FORMAT));
            memoryDTO.setAiResponseFlag(CommonConstants.NO_STR);
            memoryDTO.setMemoryLeafDepth(0);
            memoryDTO.setMessageLastAccessTime(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_FORMAT));
            memoryDTO.setMessageParentIds(Lists.newArrayList("0"));
            memoryDTO.setUseToken(OpenAiTokenizerUtil.getMessageToken(convertMemoryMsg2ModelMsg(memoryDTO)));
            String lastMsgIdMapKey = memoryDTO.getMessageOwnerId() + DOUBLE_COLON + memoryDTO.getMessageCreatorId();
            String msgListKey = memoryDTO.getMessageOwnerId() + DOUBLE_COLON + memoryDTO.getMessageCreatorId() + Constants.MSG_LIST_KEY_SUFFIX;
            RedisLikeCounter.addMsg(msgListKey,
                    MemoryDTO.builder().messageId(memoryDTO.getMessageId()).realCreatorId(memoryDTO.getRealCreatorId()).groupMsgFlag(memoryDTO.getGroupMsgFlag()).messageContentType(memoryDTO.getMessageContentType()).aiResponseFlag(memoryDTO.getAiResponseFlag()).messageContent(memoryDTO.getMessageContent()).build());
            // 异步插入用户消息
            CHAT_POOL.execute(() -> MemoryInsert.insertNewMemory(memoryDTO));
            // 更新最后一条消息id
            LAST_MESSAGE_ID_MAP.get().put(lastMsgIdMapKey, memoryDTO.getMessageId());
            // 检查是否是最后一条消息
            if (checkLastMessageId(memoryDTO)) {
                return null;
            }
            List<MemoryDTO> memoryDTOList = StringUtils.equals(memoryDTO.getMessageContentType(), ContentTypeEnum.TEXT.getType()) ? MemorySearch.searchRelationMemory(memoryDTO.getMessageOwnerId(),
                    memoryDTO.getMessageCreatorId(), memoryDTO.getMessageContent()) : new ArrayList<>();
            LinkedList<ChatMessage> messageList = new LinkedList<>();
            List<MemoryDTO> memoryDTOS = RedisLikeCounter.getMsg(msgListKey);
            log.info("msgListKey :{} ", JSON.toJSONString(memoryDTOS));
            if (checkLastMessageId(memoryDTO)) {
                return null;
            }
            List<String> existMsgIds = getMinMemoryContext(msgListKey, messageList, memoryDTOS);
            if (checkLastMessageId(memoryDTO)) {
                return null;
            }
            // 选择prompt
            SystemMessage systemMessage;
            boolean groupFlag = StringUtils.equals(memoryDTO.getGroupMsgFlag(), YES_STR);
            String now = DateUtil.format(new Date(), DatePattern.NORM_DATETIME_FORMAT);
            if (CollectionUtils.isEmpty(memoryDTOList)) {
                systemMessage = new SystemMessage(String.format(groupFlag ? GROUP_PROMPT_PREFIX : PROMPT_PREFIX, memoryDTO.getMessageCreatorName()) + String.format(groupFlag ? GROUP_PROMPT_END :
                        PROMPT_END, now));
            }else {
                StringBuilder memory = new StringBuilder();
                for (int i = 1; i < memoryDTOList.size(); i++) {
                    MemoryDTO memorySingle = memoryDTOList.get(i);
                    if (existMsgIds.contains(memorySingle.getMessageId())) {
                        continue;
                    }
                    memory.append(i).append("(").append(memorySingle.getMessageCreateAt()).append(")").append(Optional.ofNullable(memorySingle.getRealCreatorName()).orElse(memorySingle.getMessageCreatorName())).append(":").append(memorySingle.getMessageContent()).append("\n");
                    MemoryUpdate.updateMemoryAccessTime(memorySingle.getMessageId());
                }
                systemMessage = new SystemMessage(String.format(groupFlag ? GROUP_PROMPT_PREFIX : PROMPT_PREFIX, memoryDTO.getMessageCreatorName()) + (StringUtils.isNotBlank(memory) ?
                        String.format(groupFlag ? GROUP_PROMPT_MID : PROMPT_MID, memory) : "") + String.format(groupFlag ? GROUP_PROMPT_END : PROMPT_END, now));
            }
            messageList.addFirst(systemMessage);
            List<OpenAiApi.ChatCompletionMessage> springAiMessages = convertMessage(messageList);
            if (checkLastMessageId(memoryDTO)) {
                return null;
            }
            // 对话
            OpenAiApi.ChatCompletion aiMessageResponse = springAiChat.generateMsgWithMsgListAndFunctions(springAiMessages);
            if (aiMessageResponse == null || CollectionUtils.isEmpty(aiMessageResponse.choices())) {
                return null;
            }
            log.info("ai response:{}", JSON.toJSONString(aiMessageResponse.choices().get(0)));
            if (checkLastMessageId(memoryDTO)) {
                return null;
            }
            CHAT_POOL.execute(() -> {
                List<MemoryDTO> aiMsgDTOList = convertSpringMsg2AiMSg(aiMessageResponse.choices().get(0).message(), memoryDTO, aiMessageResponse.usage().completionTokens());
                aiMsgDTOList.forEach(MemoryInsert::insertNewMemory);
            });
            //            if (messageList.getLast() instanceof ToolExecutionResultMessage resultMessage) {
            //                if (resultMessage.toolName().equals(GENERATE_IMAGE_FUNCTION_NAME)) {
            //                    return new ChatResponse(ContentTypeEnum.PICTURE.getType(), resultMessage.text());
            //                }
            //            }
            //            return new ChatResponse(ContentTypeEnum.TEXT.getType(), aiMessageResponse.content().text());
            ChatResponse chatResponse = new ChatResponse();
            List<SendMessageRequest.SendMessage> sendMessageList = new ArrayList<>();
            if (aiMessageResponse.choices().get(0).message() != null) {
                OpenAiApi.ChatCompletionMessage aiMessage = aiMessageResponse.choices().get(0).message();
                if (CollectionUtils.isEmpty(aiMessage.toolCalls())) {
                    SendMessageRequest.SendMessage sendMessage = new SendMessageRequest.SendMessage();
                    sendMessage.setContent(aiMessage.content());
                    sendMessage.setContentType(ContentTypeEnum.TEXT.getType());
                    sendMessageList.add(sendMessage);
                }else {
                    for (OpenAiApi.ChatCompletionMessage.ToolCall toolExecutionRequest : aiMessage.toolCalls()) {
                        if (toolExecutionRequest.function().name().equals("replyMessageProcessing")) {
                            if (StringUtils.isNotBlank(toolExecutionRequest.function().arguments())) {
                                SendMessageRequest sendMessageRequest = JSON.parseObject(toolExecutionRequest.function().arguments(), SendMessageRequest.class);
                                if (sendMessageRequest.isNeedsSending() && CollectionUtils.isNotEmpty(sendMessageRequest.getSendMessageList())) {
                                    sendMessageList.addAll(sendMessageRequest.getSendMessageList());
                                }
                            }
                        }
                    }
                }
                chatResponse.setSendMessageList(sendMessageList);
            }
            return chatResponse;
        } catch (Exception e) {
            log.error("error", e);
            return null;
        }
    }


    private List<OpenAiApi.ChatCompletionMessage> convertMessage(LinkedList<ChatMessage> messageList) {
        List<OpenAiApi.ChatCompletionMessage> chatCompletionMessages = new ArrayList<>();

        for (ChatMessage chatMessage : messageList) {
            OpenAiApi.ChatCompletionMessage message = null;
            if (chatMessage instanceof SystemMessage) {
                message = new OpenAiApi.ChatCompletionMessage(((SystemMessage) chatMessage).text(), OpenAiApi.ChatCompletionMessage.Role.SYSTEM);
            }else if (chatMessage instanceof ToolExecutionResultMessage) {
                ToolExecutionResultMessage toolExecutionResultMessage = (ToolExecutionResultMessage) chatMessage;
                message = new OpenAiApi.ChatCompletionMessage(toolExecutionResultMessage.text(), OpenAiApi.ChatCompletionMessage.Role.TOOL, toolExecutionResultMessage.toolName(),
                        toolExecutionResultMessage.id(), null);
            }else if (chatMessage instanceof AiMessage) {
                message = new OpenAiApi.ChatCompletionMessage(chatMessage.text(), OpenAiApi.ChatCompletionMessage.Role.ASSISTANT);
            }else if (chatMessage instanceof UserMessage) {
                UserMessage userMessage = (UserMessage) chatMessage;
                for (Content content : userMessage.contents()) {
                    if (content instanceof TextContent) {
                        message = new OpenAiApi.ChatCompletionMessage(new OpenAiApi.ChatCompletionMessage.MediaContent(((TextContent) content).text()), OpenAiApi.ChatCompletionMessage.Role.USER);
                    }else if (content instanceof ImageContent) {
                        message = new OpenAiApi.ChatCompletionMessage(new OpenAiApi.ChatCompletionMessage.MediaContent.ImageUrl(((ImageContent) content).image().base64Data(),
                                ((ImageContent) content).detailLevel().name().toLowerCase()), OpenAiApi.ChatCompletionMessage.Role.USER);
                    }
                }
            }else {
                continue;
            }
            chatCompletionMessages.add(message);
        }
        return chatCompletionMessages;
    }


    private List<MemoryDTO> convertSpringMsg2AiMSg(OpenAiApi.ChatCompletionMessage aiMessage, MemoryDTO memoryDTO, Integer token) {
        List<MemoryDTO> memoryDTOS = new ArrayList<>();
        if (aiMessage != null) {
            if (CollectionUtils.isEmpty(aiMessage.toolCalls())) {
                MemoryDTO aiMemoryDTO = new MemoryDTO();
                aiMemoryDTO.setMessageCreatorId(CreatorEnum.Andrew.getUserId());
                aiMemoryDTO.setMessageCreatorName(CreatorEnum.Andrew.getUserName());
                aiMemoryDTO.setMessageCreatorType(CreatorEnum.Andrew.getType());
                aiMemoryDTO.setMessageReceiveId(memoryDTO.getMessageCreatorId());
                aiMemoryDTO.setMessageReceiveName(memoryDTO.getMessageCreatorName());
                aiMemoryDTO.setMessageReceiveType(memoryDTO.getMessageCreatorType());
                aiMemoryDTO.setMessageOwnerId(memoryDTO.getMessageOwnerId());
                aiMemoryDTO.setMessageOwnerName(memoryDTO.getMessageOwnerName());
                aiMemoryDTO.setMessageOwnerType(memoryDTO.getMessageOwnerType());
                aiMemoryDTO.setMessageCreateAt(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_FORMAT));
                aiMemoryDTO.setMessageContentType(ContentTypeEnum.TEXT.getType());
                aiMemoryDTO.setMessageContent(aiMessage.content());
                aiMemoryDTO.setMemoryLeafDepth(0);
                aiMemoryDTO.setMessageLastAccessTime(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_FORMAT));
                aiMemoryDTO.setUseToken(token);
                aiMemoryDTO.setMessageParentIds(Lists.newArrayList(memoryDTO.getMessageId()));
                aiMemoryDTO.setAiResponseFlag(YES_STR);
                aiMemoryDTO.setGroupMsgFlag(memoryDTO.getGroupMsgFlag());
                aiMemoryDTO.setRealCreatorId(CreatorEnum.Andrew.getUserId());
                aiMemoryDTO.setRealCreatorName(CreatorEnum.Andrew.getUserName());
                memoryDTOS.add(aiMemoryDTO);
            }else {
                for (OpenAiApi.ChatCompletionMessage.ToolCall toolExecutionRequest : aiMessage.toolCalls()) {
                    if (toolExecutionRequest.function().name().equals("sendWechatMessage")) {
                        if (StringUtils.isNotBlank(toolExecutionRequest.function().name())) {
                            SendMessageRequest sendMessageRequest = JSON.parseObject(toolExecutionRequest.function().arguments(), SendMessageRequest.class);
                            if (sendMessageRequest.isNeedsSending() && CollectionUtils.isNotEmpty(sendMessageRequest.getSendMessageList())) {
                                for (SendMessageRequest.SendMessage sendMessage : sendMessageRequest.getSendMessageList()) {
                                    MemoryDTO aiMemoryDTO = new MemoryDTO();
                                    aiMemoryDTO.setMessageCreatorId(CreatorEnum.Andrew.getUserId());
                                    aiMemoryDTO.setMessageCreatorName(CreatorEnum.Andrew.getUserName());
                                    aiMemoryDTO.setMessageCreatorType(CreatorEnum.Andrew.getType());
                                    aiMemoryDTO.setMessageReceiveId(memoryDTO.getMessageCreatorId());
                                    aiMemoryDTO.setMessageReceiveName(memoryDTO.getMessageCreatorName());
                                    aiMemoryDTO.setMessageReceiveType(memoryDTO.getMessageCreatorType());
                                    aiMemoryDTO.setMessageOwnerId(memoryDTO.getMessageOwnerId());
                                    aiMemoryDTO.setMessageOwnerName(memoryDTO.getMessageOwnerName());
                                    aiMemoryDTO.setMessageOwnerType(memoryDTO.getMessageOwnerType());
                                    aiMemoryDTO.setMessageCreateAt(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_FORMAT));
                                    aiMemoryDTO.setMessageContentType(sendMessage.getContentType());
                                    aiMemoryDTO.setMessageContent(sendMessage.getContent());
                                    aiMemoryDTO.setMemoryLeafDepth(0);
                                    aiMemoryDTO.setMessageLastAccessTime(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_FORMAT));
                                    aiMemoryDTO.setUseToken(token);
                                    aiMemoryDTO.setMessageParentIds(Lists.newArrayList(memoryDTO.getMessageId()));
                                    aiMemoryDTO.setAiResponseFlag(YES_STR);
                                    aiMemoryDTO.setGroupMsgFlag(memoryDTO.getGroupMsgFlag());
                                    aiMemoryDTO.setRealCreatorId(CreatorEnum.Andrew.getUserId());
                                    aiMemoryDTO.setRealCreatorName(CreatorEnum.Andrew.getUserName());
                                    memoryDTOS.add(aiMemoryDTO);
                                }
                            }
                        }
                    }
                }
            }
        }
        return memoryDTOS;
    }


    public void convertAudio2TextMsg(BaseMemoryDTO baseMemoryDTO) {
        if (!StringUtils.equals(baseMemoryDTO.getMessageContentType(), ContentTypeEnum.AUDIO.getType())) {
            return;
        }
        //        byte[] fileBytes = Base64.getDecoder().decode(baseMemoryDTO.getMessageContent());
        //        // 将 byte[] 转换为 Resource 对象
        //        Resource resource = new ByteArrayResource(fileBytes);
        DownloadTools.awaitDownload(baseMemoryDTO.getMessageContent());
        baseMemoryDTO.setMessageContentType(ContentTypeEnum.TEXT.getType());
        baseMemoryDTO.setMessageContent(springAiAudio.generateTextWithAudio(new FileSystemResource(baseMemoryDTO.getMessageContent())));
    }


    public static ChatMessage convertMemoryMsg2ModelMsg(BaseMemoryDTO baseMemoryDTO) {
        String contentPrefix = StringUtils.equals(baseMemoryDTO.getGroupMsgFlag(), YES_STR) ? baseMemoryDTO.getRealCreatorId() + ":" : "";
        if (baseMemoryDTO.getMessageContentType().equals(ContentTypeEnum.TEXT.getType())) {
            return UserMessage.from(contentPrefix + baseMemoryDTO.getMessageContent());
        }else if (baseMemoryDTO.getMessageContentType().equals(ContentTypeEnum.PICTURE.getType())) {
            return new UserMessage(new ImageContent(new Image.Builder().mimeType(IMAGE_TYPE).base64Data(getFileBase64Data(baseMemoryDTO.getMessageContent())).build()));
        }else if (baseMemoryDTO.getMessageContentType().equals(ContentTypeEnum.NOTE.getType())) {
            return new SystemMessage(baseMemoryDTO.getMessageContent());
        }
        return null;
    }


    public static String getFileBase64Data(String filePath) {
        try {
            DownloadTools.awaitDownload(filePath);
            // 读取文件内容到字节数组
            byte[] fileContent = Files.readAllBytes(Paths.get(filePath));

            // 将字节数组编码为 Base64 字符串
            String base64String = Base64.getEncoder().encodeToString(fileContent);

            // 输出 Base64 字符串
            return base64String;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private static List<String> getMinMemoryContext(String msgListKey, LinkedList<ChatMessage> messageList, List<MemoryDTO> memoryDTOS) {
        if (CollectionUtils.isEmpty(memoryDTOS)) {
            return new ArrayList<>();
        }
        List<String> existMsgIdList = new ArrayList<>();
        int sumMsgToken = 0;
        for (int i = memoryDTOS.size() - 1; i >= 0; i--) {
            MemoryDTO memoryDTO = memoryDTOS.get(i);
            ChatMessage chatMessage;
            if (memoryDTO.getAiResponseFlag().equals(YES_STR)) {
                chatMessage = new AiMessage(memoryDTO.getMessageContent());
                sumMsgToken = sumMsgToken + OpenAiTokenizerUtil.getMessageToken(chatMessage);
            }else {
                chatMessage = convertMemoryMsg2ModelMsg(memoryDTO);
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


    private static List<MemoryDTO> convert2AiMSg(AiMessage aiMessage, MemoryDTO memoryDTO, int token) {
        List<MemoryDTO> memoryDTOS = new ArrayList<>();
        if (aiMessage != null) {
            if (CollectionUtils.isEmpty(aiMessage.toolExecutionRequests())) {
                MemoryDTO aiMemoryDTO = new MemoryDTO();
                aiMemoryDTO.setMessageCreatorId(CreatorEnum.Andrew.getUserId());
                aiMemoryDTO.setMessageCreatorName(CreatorEnum.Andrew.getUserName());
                aiMemoryDTO.setMessageCreatorType(CreatorEnum.Andrew.getType());
                aiMemoryDTO.setMessageReceiveId(memoryDTO.getMessageCreatorId());
                aiMemoryDTO.setMessageReceiveName(memoryDTO.getMessageCreatorName());
                aiMemoryDTO.setMessageReceiveType(memoryDTO.getMessageCreatorType());
                aiMemoryDTO.setMessageOwnerId(memoryDTO.getMessageOwnerId());
                aiMemoryDTO.setMessageOwnerName(memoryDTO.getMessageOwnerName());
                aiMemoryDTO.setMessageOwnerType(memoryDTO.getMessageOwnerType());
                aiMemoryDTO.setMessageCreateAt(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_FORMAT));
                aiMemoryDTO.setMessageContentType(ContentTypeEnum.TEXT.getType());
                aiMemoryDTO.setMessageContent(aiMessage.text());
                aiMemoryDTO.setMemoryLeafDepth(0);
                aiMemoryDTO.setMessageLastAccessTime(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_FORMAT));
                aiMemoryDTO.setUseToken(token);
                aiMemoryDTO.setMessageParentIds(Lists.newArrayList(memoryDTO.getMessageId()));
                aiMemoryDTO.setAiResponseFlag(YES_STR);
                aiMemoryDTO.setGroupMsgFlag(memoryDTO.getGroupMsgFlag());
                aiMemoryDTO.setRealCreatorId(CreatorEnum.Andrew.getUserId());
                aiMemoryDTO.setRealCreatorName(CreatorEnum.Andrew.getUserName());
                memoryDTOS.add(aiMemoryDTO);
            }else {
                for (ToolExecutionRequest toolExecutionRequest : aiMessage.toolExecutionRequests()) {
                    if (toolExecutionRequest.name().equals("replyMessageProcessing")) {
                        if (StringUtils.isNotBlank(toolExecutionRequest.arguments())) {
                            SendMessageRequest sendMessageRequest = JSON.parseObject(toolExecutionRequest.arguments(), SendMessageRequest.class);
                            if (sendMessageRequest.isNeedsSending() && CollectionUtils.isNotEmpty(sendMessageRequest.getSendMessageList())) {
                                for (SendMessageRequest.SendMessage sendMessage : sendMessageRequest.getSendMessageList()) {
                                    MemoryDTO aiMemoryDTO = new MemoryDTO();
                                    aiMemoryDTO.setMessageCreatorId(CreatorEnum.Andrew.getUserId());
                                    aiMemoryDTO.setMessageCreatorName(CreatorEnum.Andrew.getUserName());
                                    aiMemoryDTO.setMessageCreatorType(CreatorEnum.Andrew.getType());
                                    aiMemoryDTO.setMessageReceiveId(memoryDTO.getMessageCreatorId());
                                    aiMemoryDTO.setMessageReceiveName(memoryDTO.getMessageCreatorName());
                                    aiMemoryDTO.setMessageReceiveType(memoryDTO.getMessageCreatorType());
                                    aiMemoryDTO.setMessageOwnerId(memoryDTO.getMessageOwnerId());
                                    aiMemoryDTO.setMessageOwnerName(memoryDTO.getMessageOwnerName());
                                    aiMemoryDTO.setMessageOwnerType(memoryDTO.getMessageOwnerType());
                                    aiMemoryDTO.setMessageCreateAt(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_FORMAT));
                                    aiMemoryDTO.setMessageContentType(sendMessage.getContentType());
                                    aiMemoryDTO.setMessageContent(sendMessage.getContent());
                                    aiMemoryDTO.setMemoryLeafDepth(0);
                                    aiMemoryDTO.setMessageLastAccessTime(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_FORMAT));
                                    aiMemoryDTO.setUseToken(token);
                                    aiMemoryDTO.setMessageParentIds(Lists.newArrayList(memoryDTO.getMessageId()));
                                    aiMemoryDTO.setAiResponseFlag(YES_STR);
                                    aiMemoryDTO.setGroupMsgFlag(memoryDTO.getGroupMsgFlag());
                                    aiMemoryDTO.setRealCreatorId(CreatorEnum.Andrew.getUserId());
                                    aiMemoryDTO.setRealCreatorName(CreatorEnum.Andrew.getUserName());
                                    memoryDTOS.add(aiMemoryDTO);
                                }
                            }
                        }
                    }
                }
            }
        }
        return memoryDTOS;
    }

}
