package com.github.hambuger.memory.chat.memory.chat;

import com.github.hambuger.memory.chat.memory.chat.dto.ExtraBaseMemoryDTO;
import com.github.hambuger.memory.chat.wechat.api.MessageTools;
import com.github.hambuger.memory.chat.wechat.entity.Message;
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
import com.github.hambuger.memory.chat.memory.wechat.SendMessage;
import com.github.hambuger.memory.chat.memory.wechat.SendMessageRequest;
import com.github.hambuger.memory.chat.wechat.api.DownloadTools;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
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
import static com.github.hambuger.memory.chat.memory.constants.Constants.EMOJI_TYPE;
import static com.github.hambuger.memory.chat.memory.constants.Constants.IMAGE_TYPE;
import static com.github.hambuger.memory.chat.memory.constants.Constants.REPLY_MESSAGE_FUNCTION_NAME;
import static java.lang.String.format;


/**
 * @author hamburger
 * @since 2024/6/3
 */
@Slf4j
@Component
public class ChatCompletionsApi {

    private static final ThreadPoolExecutor CHAT_POOL = new ThreadPoolExecutor(10, 20, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000), new CustomizableThreadFactory("chat-pool"),
            new ThreadPoolExecutor.CallerRunsPolicy());


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


    public ChatResponse chat(ExtraBaseMemoryDTO baseMemoryDTO) {
        try {
            log.info("get a new msg:{}", JSON.toJSONString(baseMemoryDTO));
            MemoryDTO memoryDTO = getChatMemory(baseMemoryDTO);
            String lastMsgIdMapKey = memoryDTO.getMessageOwnerId() + DOUBLE_COLON + memoryDTO.getMessageCreatorId();
            String msgListKey = memoryDTO.getMessageOwnerId() + DOUBLE_COLON + memoryDTO.getMessageCreatorId() + Constants.MSG_LIST_KEY_SUFFIX;
            RedisLikeCounter.addMsg(msgListKey,
                    MemoryDTO.builder().messageId(memoryDTO.getMessageId()).messageCreateAt(memoryDTO.getMessageCreateAt()).realCreatorId(memoryDTO.getRealCreatorId()).messageCreatorId(memoryDTO.getMessageCreatorId()).groupMsgFlag(memoryDTO.getGroupMsgFlag()).messageContentType(memoryDTO.getMessageContentType()).aiResponseFlag(memoryDTO.getAiResponseFlag()).messageContent(memoryDTO.getMessageContent()).build());
            // 异步插入用户消息
            CHAT_POOL.execute(() -> MemoryInsert.insertNewMemory(memoryDTO));
            // 更新最后一条消息id
            LAST_MESSAGE_ID_MAP.get().put(lastMsgIdMapKey, memoryDTO.getMessageId());
            // 检查是否是最后一条消息
            if (checkLastMessageId(memoryDTO)) {
                return null;
            }
            // 查询相关性最高的历史消息
            List<MemoryDTO> searchMemoryList = StringUtils.equals(memoryDTO.getMessageContentType(), ContentTypeEnum.TEXT.getType()) ? MemorySearch.searchRelationMemory(memoryDTO.getMessageOwnerId(),
                    memoryDTO.getMessageCreatorId(), memoryDTO.getMessageContent()) : new ArrayList<>();
            LinkedList<ChatMessage> messageList = new LinkedList<>();
            List<MemoryDTO> memoryDTOS = RedisLikeCounter.getMsg(msgListKey);
            if (checkLastMessageId(memoryDTO)) {
                return null;
            }
            // 获取最近时间的聊天记录
            List<String> existMsgIds = getMinMemoryContext(msgListKey, messageList, memoryDTOS);
            if (checkLastMessageId(memoryDTO)) {
                return null;
            }
            SystemMessage systemMessage = getSystemMessage(memoryDTO, searchMemoryList, existMsgIds);
            messageList.addFirst(systemMessage);
            List<OpenAiApi.ChatCompletionMessage> springAiMessages = convertMessage(messageList);
            if (checkLastMessageId(memoryDTO)) {
                return null;
            }
            // 获取AI回复
            OpenAiApi.ChatCompletion aiMessageResponse = springAiChat.generateMsgWithMsgListAndFunctions(springAiMessages);
            if (aiMessageResponse == null || CollectionUtils.isEmpty(aiMessageResponse.choices())) {
                return null;
            }
            OpenAiApi.ChatCompletionMessage responseMessage = aiMessageResponse.choices().get(0).message();
            log.info("ai response:{}", responseMessage);
            if (checkLastMessageId(memoryDTO)) {
                return null;
            }
            CHAT_POOL.execute(() -> {
                List<MemoryDTO> aiMsgDTOList = convertSpringMsg2AiMSg(responseMessage, memoryDTO, aiMessageResponse.usage().completionTokens());
                aiMsgDTOList.forEach(MemoryInsert::insertNewMemory);
            });
            ChatResponse chatResponse = new ChatResponse();
            // 转换成发送消息
            List<SendMessage> sendMessageList = convertSendMessageList(responseMessage);
            if (CollectionUtils.isEmpty(sendMessageList)) {
                return null;
            } else {
                startNewTaskForContact(baseMemoryDTO.getFromUserName(), memoryDTO, msgListKey);
            }
            chatResponse.setSendMessageList(sendMessageList);
            return chatResponse;
        } catch (Exception e) {
            log.error("error", e);
            return null;
        }
    }

    private void startNewTaskForContact(String toUserId, MemoryDTO memoryDTO, String msgListKey) {
        StartConversationCheckTask.startTaskForContact(msgListKey, () -> {
            List<MemoryDTO> memoryDTOS = RedisLikeCounter.getMsg(msgListKey);
            if(CollectionUtils.isEmpty(memoryDTOS)){
                return false;
            }
            String prompt = getCheckStartMsgPrompt(memoryDTO.getMessageCreatorName(), memoryDTOS);
            if(StringUtils.isBlank(prompt)){
                return false;
            }
            String aiResponse = LangChainChat.generateJsonWithSingleMsgAndPrompt(prompt);
            if(StringUtils.isBlank(aiResponse)){
                return false;
            }
            SendMessageRequest sendMessageRequest = JSON.parseObject(aiResponse, SendMessageRequest.class);
            if(sendMessageRequest == null || !sendMessageRequest.isNeedsSending() || (CollectionUtils.isEmpty(sendMessageRequest.getSendPictureMessageList()) && CollectionUtils.isEmpty(sendMessageRequest.getSendTextMessageList()))){
                return false;
            }
            List<Message> sendMessageList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(sendMessageRequest.getSendTextMessageList())) {
                for (String text : sendMessageRequest.getSendTextMessageList()) {
                    Message sendMessage = new Message();
                    sendMessage.setToUsername(toUserId);
                    sendMessage.setContent(text);
                    sendMessage.setMsgType(ContentTypeEnum.TEXT.getMsgType());
                    sendMessageList.add(sendMessage);
                    MemoryDTO aiMemoryDTO = getAiResponseMemoryDTO(memoryDTO, ContentTypeEnum.TEXT.getType(), text, 0);
                    CHAT_POOL.execute(() ->MemoryInsert.insertNewMemory(aiMemoryDTO));
                }
            }else{
                return false;
            }
            MessageTools.sendMsgByUserId(sendMessageList);
            return true;
        });
    }

    private String getCheckStartMsgPrompt(String toUserName, List<MemoryDTO> memoryDTOS) {
        try {
            StringBuilder promptBuilder = new StringBuilder().append("你是Andrew,下面是你和%s的离当前时间最近的对话内容。【%s】\n判断一下是否需要给%s发送一个新的消息。\n")
                    .append("应该是在确实有必要的情况下才发起会话,尽量不要打扰别人，尤其是在夜晚时间。长时间没有联系的人也要谨慎判断是否发起消息。\n")
                    .append("即使需要发送消息，也不要频繁发送重复的信息\n")
                    .append("如果需要发送消息，给出需要发送的消息内容。你给出的发送消息的判断和消息内容应该类似如下的json格式：\n %s\n")
                    .append("其中needsSending字段表示是否需要发送消息，sendTextMessageList字段表示需要发送的消息内容\n")
                    .append("注意：现在时间是%s");
            SendMessageRequest sendMessageRequest = new SendMessageRequest();
            sendMessageRequest.setNeedsSending(false);
            sendMessageRequest.setSendTextMessageList(new ArrayList<>());
            String msgJsonStr = JSON.toJSONString(sendMessageRequest);
            StringBuilder memoryStr = new StringBuilder();
            for (int i = 1; i < memoryDTOS.size(); i++) {
                MemoryDTO memorySingle = memoryDTOS.get(i);
                memoryStr.append(i).append(".(").append(memorySingle.getMessageCreateAt()).append(")").append(Optional.ofNullable(memorySingle.getRealCreatorId()).orElse(Optional.ofNullable(memorySingle.getMessageCreatorId()).orElse(CreatorEnum.Andrew.getUserName()))).append(":").append(memorySingle.getMessageContent()).append("\n");
            }
            return String.format(promptBuilder.toString(), toUserName, memoryStr, toUserName, msgJsonStr, DateUtil.format(new Date(), DatePattern.NORM_DATETIME_FORMAT));
        } catch (Exception e) {
            log.error("getCheckStartMsgPrompt error", e);
        }
        return null;
    }


    private static @NotNull List<SendMessage> convertSendMessageList(OpenAiApi.ChatCompletionMessage responseMessage) {
        List<SendMessage> sendMessageList = new ArrayList<>();
        if (responseMessage != null) {
            if (CollectionUtils.isEmpty(responseMessage.toolCalls())) {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setMessageContent(responseMessage.content());
                sendMessage.setMessageContentType(ContentTypeEnum.TEXT.getType());
                sendMessageList.add(sendMessage);
            }else {
                for (OpenAiApi.ChatCompletionMessage.ToolCall toolExecutionRequest : responseMessage.toolCalls()) {
                    if (toolExecutionRequest.function().name().equals(REPLY_MESSAGE_FUNCTION_NAME)) {
                        if (StringUtils.isNotBlank(toolExecutionRequest.function().arguments())) {
                            SendMessageRequest sendMessageRequest = JSON.parseObject(toolExecutionRequest.function().arguments(), SendMessageRequest.class);
                            if (sendMessageRequest.isNeedsSending()) {
                                if (CollectionUtils.isNotEmpty(sendMessageRequest.getSendTextMessageList())) {
                                    for (String text : sendMessageRequest.getSendTextMessageList()) {
                                        SendMessage sendMessage = new SendMessage();
                                        sendMessage.setMessageContent(text);
                                        sendMessage.setMessageContentType(ContentTypeEnum.TEXT.getType());
                                        sendMessageList.add(sendMessage);
                                    }
                                }
                                if (CollectionUtils.isNotEmpty(sendMessageRequest.getSendPictureMessageList())) {
                                    for (String pic : sendMessageRequest.getSendPictureMessageList()) {
                                        SendMessage sendMessage = new SendMessage();
                                        sendMessage.setMessageContent(pic);
                                        sendMessage.setMessageContentType(ContentTypeEnum.PICTURE.getType());
                                        sendMessageList.add(sendMessage);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return sendMessageList;
    }


    private static @NotNull SystemMessage getSystemMessage(MemoryDTO memoryDTO, List<MemoryDTO> searchMemoryList, List<String> existMsgIds) {
        SystemMessage systemMessage;
        boolean groupFlag = StringUtils.equals(memoryDTO.getGroupMsgFlag(), YES_STR);
        String now = DateUtil.format(new Date(), DatePattern.NORM_DATETIME_FORMAT);
        // 选择prompt
        if (CollectionUtils.isEmpty(searchMemoryList)) {
            systemMessage = new SystemMessage(String.format(groupFlag ? Constants.GROUP_PROMPT_PREFIX : Constants.PROMPT_PREFIX, memoryDTO.getMessageCreatorName()) + String.format(groupFlag ? Constants.GROUP_PROMPT_END :
                    Constants.PROMPT_END, now));
        }else {
            StringBuilder memory = new StringBuilder();
            for (int i = 1; i < searchMemoryList.size(); i++) {
                MemoryDTO memorySingle = searchMemoryList.get(i);
                if (existMsgIds.contains(memorySingle.getMessageId())) {
                    continue;
                }
                memory.append(i).append("(").append(memorySingle.getMessageCreateAt()).append(")").append(Optional.ofNullable(memorySingle.getRealCreatorName()).orElse(memorySingle.getMessageCreatorName())).append(":").append(memorySingle.getMessageContent()).append("\n");
                MemoryUpdate.updateMemoryAccessTime(memorySingle.getMessageId());
            }
            systemMessage = new SystemMessage(String.format(groupFlag ? Constants.GROUP_PROMPT_PREFIX : Constants.PROMPT_PREFIX, memoryDTO.getMessageCreatorName()) + (StringUtils.isNotBlank(memory) ?
                    String.format(groupFlag ? Constants.GROUP_PROMPT_MID : Constants.PROMPT_MID, memory) : "") + String.format(groupFlag ? Constants.GROUP_PROMPT_END : Constants.PROMPT_END, now));
        }
        return systemMessage;
    }


    private MemoryDTO getChatMemory(BaseMemoryDTO baseMemoryDTO) {
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
        return memoryDTO;
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
                List<OpenAiApi.ChatCompletionMessage.MediaContent> mediaContents = new ArrayList<>();
                for (Content content : userMessage.contents()) {
                    if (content instanceof TextContent) {
                        mediaContents.add(new OpenAiApi.ChatCompletionMessage.MediaContent(((TextContent) content).text()));
                    }else if (content instanceof ImageContent imageContent) {
                        mediaContents.add(new OpenAiApi.ChatCompletionMessage.MediaContent(new OpenAiApi.ChatCompletionMessage.MediaContent.ImageUrl(format("data:%s;base64,%s",
                                imageContent.image().mimeType(), imageContent.image().base64Data()), ((ImageContent) content).detailLevel().name().toLowerCase())));
                    }
                }
                message = new OpenAiApi.ChatCompletionMessage(mediaContents, OpenAiApi.ChatCompletionMessage.Role.USER);
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
                MemoryDTO aiMemoryDTO = getAiResponseMemoryDTO(memoryDTO, ContentTypeEnum.TEXT.getType(), aiMessage.content(), token);
                memoryDTOS.add(aiMemoryDTO);
            }else {
                for (OpenAiApi.ChatCompletionMessage.ToolCall toolExecutionRequest : aiMessage.toolCalls()) {
                    if (toolExecutionRequest.function().name().equals(REPLY_MESSAGE_FUNCTION_NAME)) {
                        if (StringUtils.isNotBlank(toolExecutionRequest.function().name())) {
                            SendMessageRequest sendMessageRequest = JSON.parseObject(toolExecutionRequest.function().arguments(), SendMessageRequest.class);
                            if (sendMessageRequest.isNeedsSending()) {
                                List<SendMessage> sendMessageList = new ArrayList<>();
                                if (CollectionUtils.isNotEmpty(sendMessageRequest.getSendTextMessageList())) {
                                    sendMessageRequest.getSendTextMessageList().stream().forEach(text -> sendMessageList.add(new SendMessage(text, ContentTypeEnum.TEXT.getType())));
                                }
                                if (CollectionUtils.isNotEmpty(sendMessageRequest.getSendPictureMessageList())) {
                                    sendMessageRequest.getSendPictureMessageList().stream().forEach(pic -> sendMessageList.add(new SendMessage(pic, ContentTypeEnum.PICTURE.getType())));
                                }
                                for (SendMessage sendMessage : sendMessageList) {
                                    if (StringUtils.isBlank(sendMessage.getMessageContentType()) || StringUtils.isBlank(sendMessage.getMessageContent())) {
                                        continue;
                                    }
                                    MemoryDTO aiMemoryDTO = getAiResponseMemoryDTO(memoryDTO, sendMessage.getMessageContentType(), sendMessage.getMessageContent(), token);
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

    @NotNull
    private static MemoryDTO getAiResponseMemoryDTO(MemoryDTO memoryDTO, String TEXT, String aiMessage, Integer token) {
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
        aiMemoryDTO.setMessageContentType(TEXT);
        aiMemoryDTO.setMessageContent(aiMessage);
        aiMemoryDTO.setMemoryLeafDepth(0);
        aiMemoryDTO.setMessageLastAccessTime(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_FORMAT));
        aiMemoryDTO.setUseToken(token);
        aiMemoryDTO.setMessageParentIds(Lists.newArrayList(memoryDTO.getMessageId()));
        aiMemoryDTO.setAiResponseFlag(YES_STR);
        aiMemoryDTO.setGroupMsgFlag(memoryDTO.getGroupMsgFlag());
        aiMemoryDTO.setRealCreatorId(CreatorEnum.Andrew.getUserId());
        aiMemoryDTO.setRealCreatorName(CreatorEnum.Andrew.getUserName());
        return aiMemoryDTO;
    }


    public void convertAudio2TextMsg(BaseMemoryDTO baseMemoryDTO) {
        if (!StringUtils.equals(baseMemoryDTO.getMessageContentType(), ContentTypeEnum.AUDIO.getType())) {
            return;
        }
        DownloadTools.awaitDownload(baseMemoryDTO.getMessageContent());
        baseMemoryDTO.setMessageContentType(ContentTypeEnum.TEXT.getType());
        baseMemoryDTO.setMessageContent(springAiAudio.generateTextWithAudio(new FileSystemResource(baseMemoryDTO.getMessageContent())));
    }


    public static ChatMessage convertMemoryMsg2ModelMsg(BaseMemoryDTO baseMemoryDTO) {
        String contentPrefix = StringUtils.equals(baseMemoryDTO.getGroupMsgFlag(), YES_STR) ? baseMemoryDTO.getRealCreatorId() + ":" : "";
        if (baseMemoryDTO.getMessageContentType().equals(ContentTypeEnum.TEXT.getType())) {
            return UserMessage.from(contentPrefix + baseMemoryDTO.getMessageContent());
        }else if (baseMemoryDTO.getMessageContentType().equals(ContentTypeEnum.PICTURE.getType())) {
            return new UserMessage(new ImageContent(new Image.Builder().mimeType(IMAGE_TYPE).base64Data(getFileBase64Data(baseMemoryDTO.getMessageContent())).build(), ImageContent.DetailLevel.AUTO));
        }else if (baseMemoryDTO.getMessageContentType().equals(ContentTypeEnum.EMOJI.getType())) {
            return new UserMessage(new ImageContent(new Image.Builder().mimeType(EMOJI_TYPE).base64Data(getFileBase64Data(baseMemoryDTO.getMessageContent())).build(), ImageContent.DetailLevel.AUTO));
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
                MemoryDTO aiMemoryDTO = getAiResponseMemoryDTO(memoryDTO, ContentTypeEnum.TEXT.getType(), aiMessage.text(), token);
                memoryDTOS.add(aiMemoryDTO);
            }else {
                for (ToolExecutionRequest toolExecutionRequest : aiMessage.toolExecutionRequests()) {
                    if (toolExecutionRequest.name().equals(REPLY_MESSAGE_FUNCTION_NAME)) {
                        if (StringUtils.isNotBlank(toolExecutionRequest.arguments())) {
                            SendMessageRequest sendMessageRequest = JSON.parseObject(toolExecutionRequest.arguments(), SendMessageRequest.class);
                            if (sendMessageRequest.isNeedsSending()) {
                                List<SendMessage> sendMessageList = new ArrayList<>();
                                if (CollectionUtils.isNotEmpty(sendMessageRequest.getSendTextMessageList())) {
                                    sendMessageRequest.getSendTextMessageList().stream().forEach(text -> sendMessageList.add(new SendMessage(text, ContentTypeEnum.TEXT.getType())));
                                }
                                if (CollectionUtils.isNotEmpty(sendMessageRequest.getSendPictureMessageList())) {
                                    sendMessageRequest.getSendPictureMessageList().stream().forEach(pic -> sendMessageList.add(new SendMessage(pic, ContentTypeEnum.PICTURE.getType())));
                                }
                                for (SendMessage sendMessage : sendMessageList) {
                                    MemoryDTO aiMemoryDTO = getAiResponseMemoryDTO(memoryDTO, sendMessage.getMessageContentType(), sendMessage.getMessageContent(), token);
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
