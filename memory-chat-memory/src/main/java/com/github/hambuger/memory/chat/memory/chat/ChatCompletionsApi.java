package com.github.hambuger.memory.chat.memory.chat;

import com.google.common.collect.Lists;

import com.alibaba.fastjson.JSON;
import com.github.hambuger.memory.chat.memory.audio.SpringAiAudio;
import com.github.hambuger.memory.chat.memory.chat.model.ChatMember;
import com.github.hambuger.memory.chat.memory.chat.model.ChatResponse;
import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.chat.model.ContentTypeEnum;
import com.github.hambuger.memory.chat.memory.chat.model.CreatorEnum;
import com.github.hambuger.memory.chat.memory.memory.model.SpringAiChatMessageMemoryDTO;
import com.github.hambuger.memory.chat.memory.other.constants.CommonConstants;
import com.github.hambuger.memory.chat.memory.other.constants.MemoryChatConstants;
import com.github.hambuger.memory.chat.memory.emoji.SogouEmoji;
import com.github.hambuger.memory.chat.memory.other.functionCall.CallFunctionRegistryFactory;
import com.github.hambuger.memory.chat.memory.memory.create.MemoryInsert;
import com.github.hambuger.memory.chat.memory.memory.search.MemorySearch;
import com.github.hambuger.memory.chat.memory.memory.update.MemoryUpdate;
import com.github.hambuger.memory.chat.memory.memory.model.BaseMemoryDTO;
import com.github.hambuger.memory.chat.memory.memory.model.MemoryDTO;
import com.github.hambuger.memory.chat.memory.other.prompt.PromptFactory;
import com.github.hambuger.memory.chat.memory.other.token.TokenCalculation;
import com.github.hambuger.memory.chat.memory.other.util.FileUtil;
import com.github.hambuger.memory.chat.memory.other.util.IdUtil;
import com.github.hambuger.memory.chat.memory.other.util.ImageUploadUtils;
import com.github.hambuger.memory.chat.memory.other.util.RedisUtil;
import com.github.hambuger.memory.chat.memory.other.util.VideoUtil;
import com.github.hambuger.memory.chat.memory.tools.docparse.DocParse;
import com.github.hambuger.memory.chat.memory.wechat.SendMessage;
import com.github.hambuger.memory.chat.memory.wechat.SendMessageRequest;
import com.github.hambuger.memory.chat.wechat.api.DownloadTools;
import com.github.hambuger.memory.chat.wechat.api.MessageTools;
import com.github.hambuger.memory.chat.wechat.dto.response.msg.send.WebWXSendMsgResponse;
import com.github.hambuger.memory.chat.wechat.entity.Message;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Pair;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import static com.github.hambuger.memory.chat.memory.other.constants.CommonConstants.DOUBLE_COLON;
import static com.github.hambuger.memory.chat.memory.other.constants.CommonConstants.HTTP;
import static com.github.hambuger.memory.chat.memory.other.constants.CommonConstants.NO_STR;
import static com.github.hambuger.memory.chat.memory.other.constants.CommonConstants.YES_STR;
import static com.github.hambuger.memory.chat.memory.other.constants.MemoryChatConstants.CHAT_LOCK_KEY;
import static com.github.hambuger.memory.chat.memory.other.constants.MemoryChatConstants.REPLY_MESSAGE_FUNCTION_NAME;
import static org.springframework.util.ResourceUtils.FILE_URL_PREFIX;


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

    @Value("${spring.ai.openai.chat.options.model}")
    private String modelName;

    @Value("${maxMsgToken}")
    private Integer maxMsgToken;

    @Autowired
    private SpringAiAudio springAiAudio;

    @Autowired
    private SpringAiChat springAiChat;

    @Autowired
    private VideoUtil videoUtil;

    @Resource
    private ImageUploadUtils imageUploadUtils;

    @Resource
    private MemoryInsert memoryInsert;

    @Resource
    private MemorySearch memorySearch;

    @Resource
    private MemoryUpdate memoryUpdate;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private SogouEmoji sogouEmoji;

    @Resource
    private TokenCalculation tokenCalculation;

    @Resource
    private PromptFactory promptFactory;

    @Resource
    private DocParse docParse;


    public static boolean checkLastMessageId(MemoryDTO memoryDTO) {
        String lastMsgIdMapKey = memoryDTO.getMessageOwnerId() + DOUBLE_COLON + (StringUtils.equals(memoryDTO.getAiResponseFlag(), YES_STR) ? memoryDTO.getMessageReceiveId() :
                memoryDTO.getMessageCreatorId());
        String oldMsgId = LAST_MESSAGE_ID_MAP.get().get(lastMsgIdMapKey);
        return StringUtils.isNotBlank(oldMsgId) && !StringUtils.equals(oldMsgId, StringUtils.equals(memoryDTO.getAiResponseFlag(), YES_STR) ? memoryDTO.getMessageParentIds().get(0) :
                memoryDTO.getMessageId());
    }


    public ChatResponse chat(BaseMemoryDTO baseMemoryDTO) {
        String lockKey = UUID.randomUUID().toString();
        try {
            log.info("get a new msg:{}", JSON.toJSONString(baseMemoryDTO));
            redisUtil.setString(String.format(CHAT_LOCK_KEY, baseMemoryDTO.getMessageCreatorName()), lockKey);
            SpringAiChatMessageMemoryDTO memoryDTO = getChatMemory(baseMemoryDTO);
            String lastMsgIdMapKey = memoryDTO.getMessageOwnerId() + DOUBLE_COLON + memoryDTO.getMessageCreatorId();
            String msgListKey = memoryDTO.getMessageOwnerId() + DOUBLE_COLON + memoryDTO.getMessageCreatorId() + MemoryChatConstants.MSG_LIST_KEY_SUFFIX;
            boolean groupFlag = StringUtils.equals(memoryDTO.getGroupMsgFlag(), YES_STR);
            redisUtil.addMsg(msgListKey,
                    MemoryDTO.builder().messageId(memoryDTO.getMessageId()).messageCreateAt(memoryDTO.getMessageCreateAt()).realCreatorId(memoryDTO.getRealCreatorId()).messageCreatorId(memoryDTO.getMessageCreatorId()).groupMsgFlag(memoryDTO.getGroupMsgFlag()).messageContentType(memoryDTO.getMessageContentType()).aiResponseFlag(memoryDTO.getAiResponseFlag()).messageContent(memoryDTO.getMessageContent()).build());
            if (!memoryDTO.isDealFileFlag()) {
                // 异步插入用户消息
                CHAT_POOL.execute(() -> memoryInsert.insertNewMemory(memoryDTO, false));
            }
            // 更新最后一条消息id
            LAST_MESSAGE_ID_MAP.get().put(lastMsgIdMapKey, memoryDTO.getMessageId());
            // 检查是否是最后一条消息
            if (checkLastMessageId(memoryDTO)) {
                return null;
            }
            // 查询相关性最高的历史消息
            List<MemoryDTO> searchMemoryList = StringUtils.equals(memoryDTO.getMessageContentType(), ContentTypeEnum.TEXT.getType()) ?
                    memorySearch.searchRelationMemory(memoryDTO.getMessageOwnerId(), memoryDTO.getMessageCreatorId(), memoryDTO.getMessageContent(), 0) : new ArrayList<>();

            List<MemoryDTO> memoryDTOS = redisUtil.getMsg(msgListKey);
            if (checkLastMessageId(memoryDTO)) {
                return null;
            }
            // 获取最近时间的聊天记录
            Pair<List<String>, LinkedList<OpenAiApi.ChatCompletionMessage>> listPair = getMinMemoryContext(msgListKey, memoryDTOS);
            List<String> existMsgIds = listPair.getKey();
            LinkedList<OpenAiApi.ChatCompletionMessage> messageList = listPair.getValue();
            if (checkLastMessageId(memoryDTO)) {
                return null;
            }
            OpenAiApi.ChatCompletionMessage systemMessage = getSystemMessage(memoryDTO, searchMemoryList, existMsgIds);
            messageList.addFirst(systemMessage);
            if (checkLastMessageId(memoryDTO)) {
                return null;
            }
            // 获取AI回复
            OpenAiApi.ChatCompletion aiMessageResponse = springAiChat.generateMsgWithMsgListAndFunctions(messageList, groupFlag, ChatSceneEnum.NORMAL_USER);
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
                aiMsgDTOList.forEach(dto -> memoryInsert.insertNewMemory(dto, false));
                ChatMember chatMember = new ChatMember();
                chatMember.setName(memoryDTO.getMessageCreatorName());
                chatMember.setGroupFlag(StringUtils.equals(memoryDTO.getGroupMsgFlag(), YES_STR));
                redisUtil.addMember(chatMember);
            });
            // 转换成发送消息
            List<SendMessage> sendMessageList = convertSendMessageList(responseMessage);
            if (CollectionUtils.isEmpty(sendMessageList)) {
                return null;
            }else {
                startNewTaskForContact(memoryDTO, msgListKey);
            }
            ChatResponse chatResponse = new ChatResponse();
            chatResponse.setSendMessageList(sendMessageList);
            return chatResponse;
        } catch (Exception e) {
            log.error("error", e);
            return null;
        } finally {
            redisUtil.releaseLock(String.format(CHAT_LOCK_KEY, baseMemoryDTO.getMessageCreatorName()), lockKey);
        }
    }


    public void sendWxChatMessageList(String remarkName, List<SendMessage> sendMessageList, long receiveMsgTime) {
        for (SendMessage sendMessage : sendMessageList) {
            Message message = new Message();
            message.setToRemarkname(remarkName);
            message.setContent(sendMessage.getMessageContent());
            ContentTypeEnum contentTypeEnum = ContentTypeEnum.getByType(sendMessage.getMessageContentType());
            message.setMsgType(contentTypeEnum == null ? ContentTypeEnum.TEXT.getMsgType() : contentTypeEnum.getMsgType());
            List<String> emojiTypeAndMediaId = new ArrayList<>();
            if (contentTypeEnum == ContentTypeEnum.PICTURE) {
                String filePath = FileUtil.downloadImage(sendMessage.getMessageContent());
                message.setFilePath(filePath);
                message.setContent(null);
            }else if (contentTypeEnum == ContentTypeEnum.EMOJI) {
                if (CollectionUtils.isEmpty(redisUtil.getEmojiAndMediaId(sendMessage.getMessageContent()))) {
                    String emojiPath = sogouEmoji.searchEmoji(sendMessage.getMessageContent());
                    if (StringUtils.isBlank(emojiPath)) {
                        message.setMsgType(ContentTypeEnum.TEXT.getMsgType());
                    }else {
                        message.setFilePath(emojiPath);
                        if (!emojiPath.endsWith("gif")) {
                            message.setMsgType(ContentTypeEnum.PICTURE.getMsgType());
                            emojiTypeAndMediaId.add("png");
                        }else {
                            emojiTypeAndMediaId.add("gif");
                        }
                    }
                }else {
                    List<String> list = redisUtil.getEmojiAndMediaId(sendMessage.getMessageContent());
                    if (!list.get(0).equals("gif")) {
                        message.setMsgType(ContentTypeEnum.PICTURE.getMsgType());
                    }
                    message.setMediaId(list.get(1));
                }
                message.setContent(null);
            }else if (contentTypeEnum == ContentTypeEnum.TEXT) {
                int waste = message.getContent().length() * 1000 / 4;
                if (System.currentTimeMillis() < receiveMsgTime + waste) {
                    try {
                        Thread.sleep(receiveMsgTime + waste - System.currentTimeMillis());
                    } catch (InterruptedException e) {
                        log.warn("sleep error", e);
                    }
                }
            }
            WebWXSendMsgResponse webWXSendMsgResponse = MessageTools.sendMsgByRemarkName(message);
            if (contentTypeEnum == ContentTypeEnum.EMOJI && CollectionUtils.isEmpty(redisUtil.getEmojiAndMediaId(sendMessage.getMessageContent()))) {
                emojiTypeAndMediaId.add(webWXSendMsgResponse.getMediaId());
                redisUtil.putEmojiAndMediaId(sendMessage.getMessageContent(), emojiTypeAndMediaId);
            }
            receiveMsgTime = System.currentTimeMillis();
        }
    }


    private void startNewTaskForContact(MemoryDTO memoryDTO, String msgListKey) {
        StartConversationCheckTask.startTaskForContact(msgListKey, () -> {
            try {
                redisUtil.acquireLock(String.format(CHAT_LOCK_KEY, memoryDTO.getMessageCreatorName()), memoryDTO.getMessageCreatorName(), 30 * 1000L, 60 * 1000L);
                List<MemoryDTO> memoryDTOS = redisUtil.getMsg(msgListKey);
                if (CollectionUtils.isEmpty(memoryDTOS)) {
                    return false;
                }
                boolean groupFlag = StringUtils.equals(memoryDTO.getGroupMsgFlag(), YES_STR);
                String prompt = getCheckStartMsgPrompt(memoryDTO.getMessageCreatorName(), groupFlag, memoryDTOS);
                if (StringUtils.isBlank(prompt)) {
                    return false;
                }
                List<OpenAiApi.ChatCompletionMessage> messages = new ArrayList<>();
                messages.add(new OpenAiApi.ChatCompletionMessage(prompt, OpenAiApi.ChatCompletionMessage.Role.SYSTEM));
                messages.add(new OpenAiApi.ChatCompletionMessage(String.format("距离上一次发送消息给%s已经过去了%s,中间对方没有任何回复", memoryDTO.getMessageCreatorName(), formatDuration(DateUtil.between(DateUtil.parseDateTime(memoryDTOS.get(memoryDTOS.size() - 1).getMessageCreateAt()), new Date(), DateUnit.SECOND))), OpenAiApi.ChatCompletionMessage.Role.SYSTEM));
                OpenAiApi.ChatCompletion aiResponse = springAiChat.generateMsgWithMsgListAndFunctions(messages, groupFlag, ChatSceneEnum.SCHEDULE);
                if (aiResponse == null || CollectionUtils.isEmpty(aiResponse.choices())) {
                    return false;
                }
                OpenAiApi.ChatCompletionMessage responseMessage = aiResponse.choices().get(0).message();
                if (!checkNeedSendNewMsg(responseMessage, memoryDTOS, memoryDTO.getMessageCreatorName())) {
                    return false;
                }
                log.info("ai response:{}", responseMessage);
                CHAT_POOL.execute(() -> {
                    List<MemoryDTO> aiMsgDTOList = convertSpringMsg2AiMSg(responseMessage, memoryDTO, aiResponse.usage().completionTokens());
                    aiMsgDTOList.forEach(dto -> memoryInsert.insertNewMemory(dto, true));
                });
                // 转换成发送消息
                List<SendMessage> sendMessageList = convertSendMessageList(responseMessage);
                if (CollectionUtils.isEmpty(sendMessageList)) {
                    return false;
                }else {
                    sendWxChatMessageList(memoryDTO.getMessageCreatorName(), sendMessageList, System.currentTimeMillis());
                }
                return true;
            } catch (Exception e) {
                log.error("startNewTaskForContact error", e);
                return false;
            } finally {
                redisUtil.releaseLock(String.format(CHAT_LOCK_KEY, memoryDTO.getMessageCreatorName()), memoryDTO.getMessageCreatorName());
            }
        });
    }


    private String getCheckStartMsgPrompt(String toUserName, boolean groupFlag, List<MemoryDTO> memoryDTOS) {
        try {
            StringBuilder memoryStr = getMemoryStrFromMemoryList(memoryDTOS);
            return promptFactory.getChatPrompt(toUserName, memoryStr.toString(), null, groupFlag, ChatSceneEnum.SCHEDULE);
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
                                if (CollectionUtils.isNotEmpty(sendMessageRequest.getSendEmojiMessageList())) {
                                    for (String emoji : sendMessageRequest.getSendEmojiMessageList()) {
                                        SendMessage sendMessage = new SendMessage();
                                        sendMessage.setMessageContent(emoji.replaceAll("[\\[\\]]", ""));
                                        sendMessage.setMessageContentType(ContentTypeEnum.EMOJI.getType());
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


    private @NotNull OpenAiApi.ChatCompletionMessage getSystemMessage(MemoryDTO memoryDTO, List<MemoryDTO> searchMemoryList, List<String> existMsgIds) {
        OpenAiApi.ChatCompletionMessage systemMessage;
        boolean groupFlag = StringUtils.equals(memoryDTO.getGroupMsgFlag(), YES_STR);
        // 选择prompt
        if (CollectionUtils.isEmpty(searchMemoryList)) {
            systemMessage = new OpenAiApi.ChatCompletionMessage(promptFactory.getChatPrompt(memoryDTO.getMessageCreatorName(), null, null, groupFlag, ChatSceneEnum.NORMAL_USER), OpenAiApi.ChatCompletionMessage.Role.SYSTEM);
        }else {
            StringBuilder memory = new StringBuilder();
            for (int i = 0; i < searchMemoryList.size(); i++) {
                MemoryDTO memorySingle = searchMemoryList.get(i);
                if (existMsgIds.contains(memorySingle.getMessageId())) {
                    continue;
                }
                memory.append(i).append(". (").append(memorySingle.getMessageCreateAt()).append(")").append(Optional.ofNullable(memorySingle.getRealCreatorName()).orElse(memorySingle.getMessageCreatorName())).append(":").append(memorySingle.getMessageContent()).append("\n");
                memoryUpdate.updateMemoryAccessTime(memorySingle.getMessageId());
            }
            systemMessage = new OpenAiApi.ChatCompletionMessage(promptFactory.getChatPrompt(memoryDTO.getMessageCreatorName(), memory.toString(), null, groupFlag, ChatSceneEnum.NORMAL_USER),
                    OpenAiApi.ChatCompletionMessage.Role.SYSTEM);
        }
        return systemMessage;
    }


    private SpringAiChatMessageMemoryDTO getChatMemory(BaseMemoryDTO baseMemoryDTO) throws Exception {
        SpringAiChatMessageMemoryDTO memoryDTO = new SpringAiChatMessageMemoryDTO();
        memoryDTO.setMessageId(IdUtil.generateUniqueId());
        BeanUtil.copyProperties(baseMemoryDTO, memoryDTO);
        if (StringUtils.equals(baseMemoryDTO.getMessageContentType(), ContentTypeEnum.AUDIO.getType())) {
            DownloadTools.awaitDownload(baseMemoryDTO.getMessageContent());
            memoryDTO.setMessageContentType(ContentTypeEnum.TEXT.getType());
            memoryDTO.setMessageContent(springAiAudio.generateTextWithAudio(new FileSystemResource(baseMemoryDTO.getMessageContent())));
        }else if (StringUtils.equals(baseMemoryDTO.getMessageContentType(), ContentTypeEnum.VIDEO.getType())) {
            DownloadTools.awaitDownload(baseMemoryDTO.getMessageContent());
            memoryDTO.setMessageContentType(ContentTypeEnum.NOTE.getType());
            memoryDTO.setMessageContent(String.format("%s给你发过来一个视频，正在查看中", Optional.ofNullable(baseMemoryDTO.getRealCreatorName()).orElse(baseMemoryDTO.getMessageCreatorName())));
//            baseMemoryDTO.setMessageContent(getVideoInfo(baseMemoryDTO));
        }else if(StringUtils.equals(baseMemoryDTO.getMessageContentType(), ContentTypeEnum.PICTURE.getType()) || StringUtils.equals(baseMemoryDTO.getMessageContentType(), ContentTypeEnum.EMOJI.getType())){
            DownloadTools.awaitDownload(baseMemoryDTO.getMessageContent());
            memoryDTO.setMessageContent(imageUploadUtils.uploadImg(baseMemoryDTO.getMessageContent()));
        }else if(StringUtils.equals(baseMemoryDTO.getMessageContentType(), ContentTypeEnum.APP.getType())) {
            DownloadTools.awaitDownload(baseMemoryDTO.getMessageContent());
            memoryDTO.setMessageContentType(ContentTypeEnum.NOTE.getType());
            memoryDTO.setMessageContent(String.format("%s给你发过来一个文件,文件路径：%s，正在查看中", Optional.ofNullable(baseMemoryDTO.getRealCreatorName()).orElse(baseMemoryDTO.getMessageCreatorName()), baseMemoryDTO.getMessageContent()));
//            memoryDTO.setMessageContent(getFileInfo(baseMemoryDTO));
        }
        memoryDTO.setMessageCreatorId(memoryDTO.getMessageCreatorName());
        memoryDTO.setMessageCreatorType(StringUtils.equals(YES_STR, baseMemoryDTO.getGroupMsgFlag()) ? CreatorEnum.GROUP.getType() : CreatorEnum.USER.getType());
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
        OpenAiApi.ChatCompletionMessage message = convertMemoryMsg2SpringAiModelMsg(memoryDTO);
        memoryDTO.setChatMessage(message);
        memoryDTO.setUseToken(tokenCalculation.getUserMessageToken(message));
        if (StringUtils.equals(baseMemoryDTO.getMessageContentType(), ContentTypeEnum.VIDEO.getType()) || StringUtils.equals(baseMemoryDTO.getMessageContentType(), ContentTypeEnum.APP.getType())) {
            memoryDTO.setDealFileFlag(true);
            CHAT_POOL.execute(() -> {
                String fileDesc = StringUtils.equals(baseMemoryDTO.getMessageContentType(), ContentTypeEnum.VIDEO.getType()) ? getVideoInfo(baseMemoryDTO) : getFileInfo(baseMemoryDTO);
                memoryDTO.setMessageContent(fileDesc);
                memoryInsert.insertNewMemory(memoryDTO, true);
                redisUtil.updateMsgContentById(memoryDTO.getMessageOwnerId() + DOUBLE_COLON + memoryDTO.getMessageCreatorId() + MemoryChatConstants.MSG_LIST_KEY_SUFFIX, memoryDTO.getMessageId(), fileDesc);
            });
        }
        return memoryDTO;
    }


    private String getFileInfo(BaseMemoryDTO baseMemoryDTO) {
        StringBuilder info = new StringBuilder();
        info.append(Optional.ofNullable(baseMemoryDTO.getRealCreatorName()).orElse(baseMemoryDTO.getMessageCreatorName()));
        info.append("发送过来一个文件，文件地址：");
        info.append(baseMemoryDTO.getMessageContent()).append("\n");
        info.append("文件的内容大致总结如下：\n");
        String filePath = baseMemoryDTO.getMessageContent();
        if (!StringUtils.startsWith(filePath, FILE_URL_PREFIX) && !StringUtils.startsWith(filePath, HTTP)) {
            filePath = FILE_URL_PREFIX + filePath;
        }
        info.append(docParse.summaryDoc(filePath));
        return info.toString();
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
                                if (CollectionUtils.isNotEmpty(sendMessageRequest.getSendEmojiMessageList())) {
                                    sendMessageRequest.getSendEmojiMessageList().stream().forEach(emoji -> sendMessageList.add(new SendMessage(String.format("[%s]", emoji),
                                            ContentTypeEnum.EMOJI.getType())));
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
                    } else {
                        CallFunctionRegistryFactory.executeFunctionResult(toolExecutionRequest.function().name(), toolExecutionRequest.function().arguments());
                    }
                }
            }
        }
        return memoryDTOS;
    }


    @NotNull
    public static MemoryDTO getAiResponseMemoryDTO(MemoryDTO memoryDTO, String TEXT, String aiMessage, Integer token) {
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


    public OpenAiApi.ChatCompletionMessage convertMemoryMsg2SpringAiModelMsg(BaseMemoryDTO memoryDTO) {
        OpenAiApi.ChatCompletionMessage message = null;
        ContentTypeEnum contentTypeEnum = ContentTypeEnum.getByType(memoryDTO.getMessageContentType());
        switch (contentTypeEnum) {
            case NOTE:
                message = new OpenAiApi.ChatCompletionMessage(memoryDTO.getMessageContent(), OpenAiApi.ChatCompletionMessage.Role.SYSTEM);
                break;
            case TEXT:
                message = new OpenAiApi.ChatCompletionMessage(memoryDTO.getMessageContent(), OpenAiApi.ChatCompletionMessage.Role.USER);
                break;
            case PICTURE:
                message = new OpenAiApi.ChatCompletionMessage(Lists.newArrayList(new OpenAiApi.ChatCompletionMessage.MediaContent(new OpenAiApi.ChatCompletionMessage.MediaContent.ImageUrl(memoryDTO.getMessageContent(), "auto"))), OpenAiApi.ChatCompletionMessage.Role.USER);
                break;
            case EMOJI:
                message = new OpenAiApi.ChatCompletionMessage(Lists.newArrayList(new OpenAiApi.ChatCompletionMessage.MediaContent(new OpenAiApi.ChatCompletionMessage.MediaContent.ImageUrl(memoryDTO.getMessageContent(), "auto"))), OpenAiApi.ChatCompletionMessage.Role.USER);
                break;
            default:
                break;

        }
        return message;
    }


    private String getVideoInfo(BaseMemoryDTO memoryDTO) {
        Future<List<String>> audioTask = CHAT_POOL.submit(() -> springAiAudio.generateTextFromVideo(memoryDTO.getMessageContent()));
        Future<List<String>> imageTask = CHAT_POOL.submit(() -> videoUtil.getVideoImg(memoryDTO.getMessageContent()));
        List<String> fileList = new ArrayList<>();
        try {
            List<String> audioPathAntText = audioTask.get();
            List<String> imageList = imageTask.get();
            if (CollectionUtils.isNotEmpty(audioPathAntText)) {
                fileList.add(audioPathAntText.get(0));
            }
            fileList.addAll(imageList);
            return getVideInfoText(Optional.ofNullable(memoryDTO.getRealCreatorName()).orElse(memoryDTO.getMessageCreatorName()), imageList, audioPathAntText.get(1));
        } catch (Exception e) {
            log.error("getVideoInfo error", e);
        } finally {
            try {
                for (String path : fileList) {
                    Files.delete(Paths.get(path));
                }
            } catch (IOException e) {
                log.error("getVideoInfo error", e);
            }
        }
        return null;
    }


    private String getVideInfoText(String creatorName, List<String> imageList, String audioText) {
        List<OpenAiApi.ChatCompletionMessage.MediaContent> contentList = new ArrayList<>();
        List<OpenAiApi.ChatCompletionMessage> messageList = new ArrayList<>();
        messageList.add(new OpenAiApi.ChatCompletionMessage("现在有一个视频的字幕信息和视频中的截图的图片集。你需要给出这个视频的详细描述，以便让他人能够通过这个描述理解视频的内容。回复只需要给出描述，不要有其他的多余信息.\n", OpenAiApi.ChatCompletionMessage.Role.SYSTEM));
        StringBuilder prompt = new StringBuilder();
        if (StringUtils.isNotBlank(audioText)) {
            prompt.append(String.format("视频的字幕信息如下：%s", audioText));
        }
        if (CollectionUtils.isNotEmpty(imageList)) {
            prompt.append("视频的截图集合是下面这些图片");
        }
        contentList.add(new OpenAiApi.ChatCompletionMessage.MediaContent(prompt.toString()));
        if (CollectionUtils.isNotEmpty(imageList)) {
            List<String> uploadedImageList = imageUploadUtils.uploadImageList(imageList);
            for (String image : uploadedImageList) {
                contentList.add(new OpenAiApi.ChatCompletionMessage.MediaContent(new OpenAiApi.ChatCompletionMessage.MediaContent.ImageUrl(image, "low")));
            }
        }
        messageList.add(new OpenAiApi.ChatCompletionMessage(contentList, OpenAiApi.ChatCompletionMessage.Role.USER));
        OpenAiApi.ChatCompletion response = springAiChat.generateMsgWithMsgList(messageList, false);
        String videoInfo = response.choices().get(0).message().content();
        return String.format("%s发送了一个视频。这个视频的信息如下：%s", creatorName, videoInfo);
    }

    public List<OpenAiApi.ChatCompletionMessage> getAllHistoryMessageList(String msgListKey) {
        List<MemoryDTO> memoryDTOS = redisUtil.getMsg(msgListKey);
        List<OpenAiApi.ChatCompletionMessage> chatCompletionMessageList = new ArrayList<>();
        for (int i = 0; i <= memoryDTOS.size() - 1; i++) {
            MemoryDTO memoryDTO = memoryDTOS.get(i);
            OpenAiApi.ChatCompletionMessage chatMessage;
            if (StringUtils.isBlank(memoryDTO.getMessageContentType()) || StringUtils.isBlank(memoryDTO.getMessageContent())) {
                continue;
            }
            if (memoryDTO.getAiResponseFlag().equals(YES_STR)) {
                chatMessage = new OpenAiApi.ChatCompletionMessage(memoryDTO.getMessageContent(), OpenAiApi.ChatCompletionMessage.Role.ASSISTANT);
            } else {
                chatMessage = convertMemoryMsg2SpringAiModelMsg(memoryDTO);
            }
            chatCompletionMessageList.add(chatMessage);
        }
        return chatCompletionMessageList;
    }


    private Pair<List<String>, LinkedList<OpenAiApi.ChatCompletionMessage>> getMinMemoryContext(String msgListKey, List<MemoryDTO> memoryDTOS) {
        if (CollectionUtils.isEmpty(memoryDTOS)) {
            return new Pair<>(new ArrayList<>(), new LinkedList<>());
        }
        LinkedList<OpenAiApi.ChatCompletionMessage> messageList = new LinkedList<>();
        List<String> existMsgIdList = new ArrayList<>();
        int sumMsgToken = 0;
        int msgCount = 0;
        for (int i = memoryDTOS.size() - 1; i >= 0; i--) {
            MemoryDTO memoryDTO = memoryDTOS.get(i);
            OpenAiApi.ChatCompletionMessage chatMessage;
            if (StringUtils.isBlank(memoryDTO.getMessageContentType()) || StringUtils.isBlank(memoryDTO.getMessageContent())) {
                continue;
            }
            if (memoryDTO.getAiResponseFlag().equals(YES_STR)) {
                chatMessage = new OpenAiApi.ChatCompletionMessage(memoryDTO.getMessageContent(), OpenAiApi.ChatCompletionMessage.Role.ASSISTANT);
                sumMsgToken = sumMsgToken + tokenCalculation.getUserMessageToken(chatMessage);
            }else {
                chatMessage = convertMemoryMsg2SpringAiModelMsg(memoryDTO);
                sumMsgToken = sumMsgToken + tokenCalculation.getUserMessageToken(chatMessage);
            }
            msgCount++;
            if (sumMsgToken < maxMsgToken && msgCount <= 30) {
                messageList.addFirst(chatMessage);
                existMsgIdList.add(memoryDTO.getMessageId());
            }else {
                delOldMessageFromCache(msgListKey, i);
                break;
            }

        }
        return new Pair<>(existMsgIdList, messageList);
    }


    private void delOldMessageFromCache(String msgListKey, int i) {
        redisUtil.delOldMemory(msgListKey, i);
    }

    public static String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds + "秒";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            return minutes + "分钟";
        } else if (seconds < 86400) { // 一天有 86400 秒
            long hours = seconds / 3600;
            return hours + "小时";
        } else {
            long days = seconds / 86400;
            return days + "天";
        }
    }

    public void executeSchedulerTask(ChatMember chatMember, String news) {

        String memberName = chatMember.getName();
        boolean groupFlag = chatMember.isGroupFlag();
        String msgListKey = CreatorEnum.Andrew.getUserId() + DOUBLE_COLON + memberName + MemoryChatConstants.MSG_LIST_KEY_SUFFIX;
        try {
            redisUtil.acquireLock(String.format(CHAT_LOCK_KEY, memberName), memberName, 30 * 1000L, 60 * 1000L);
            List<MemoryDTO> memoryDTOS = redisUtil.getMsg(msgListKey);
            if (CollectionUtils.isEmpty(memoryDTOS)) {
                return;
            }
            String prompt = getNewsSchedulerPrompt(memberName, groupFlag, memoryDTOS, news);
            if (StringUtils.isBlank(prompt)) {
                return;
            }
            List<OpenAiApi.ChatCompletionMessage> messages = new ArrayList<>();
            messages.add(new OpenAiApi.ChatCompletionMessage(prompt, OpenAiApi.ChatCompletionMessage.Role.SYSTEM));
            messages.add(new OpenAiApi.ChatCompletionMessage(String.format("距离上一次发送消息给%s已经过去了%s,中间对方没有任何回复", chatMember.getName(), formatDuration(DateUtil.between(DateUtil.parseDateTime(memoryDTOS.get(memoryDTOS.size() - 1).getMessageCreateAt()), new Date(), DateUnit.SECOND))), OpenAiApi.ChatCompletionMessage.Role.SYSTEM));
            OpenAiApi.ChatCompletion aiResponse = springAiChat.generateMsgWithMsgListAndFunctions(messages, groupFlag, ChatSceneEnum.NEWS_SCHEDULE);
            if (aiResponse == null || CollectionUtils.isEmpty(aiResponse.choices())) {
                return;
            }
            OpenAiApi.ChatCompletionMessage responseMessage = aiResponse.choices().get(0).message();
            if (!checkNeedSendNewMsg(responseMessage, memoryDTOS, memberName)) {
                return;
            }
            log.info("ai response for news:{}", responseMessage);
            CHAT_POOL.execute(() -> {
                MemoryDTO memoryDTO = new MemoryDTO();
                memoryDTO.setMessageCreatorId(memberName);
                memoryDTO.setMessageCreatorName(memberName);
                memoryDTO.setMessageCreatorType(CreatorEnum.USER.getType());
                memoryDTO.setMessageOwnerId(CreatorEnum.Andrew.getUserId());
                memoryDTO.setMessageOwnerName(CreatorEnum.Andrew.getUserName());
                memoryDTO.setMessageOwnerType(CreatorEnum.Andrew.getType());
                memoryDTO.setGroupMsgFlag(groupFlag ? YES_STR : NO_STR);
                List<MemoryDTO> aiMsgDTOList = convertSpringMsg2AiMSg(responseMessage, memoryDTO, aiResponse.usage().completionTokens());
                aiMsgDTOList.forEach(dto -> memoryInsert.insertNewMemory(dto, true));
            });
            // 转换成发送消息
            List<SendMessage> sendMessageList = convertSendMessageList(responseMessage);
            if (CollectionUtils.isNotEmpty(sendMessageList)) {
                sendWxChatMessageList(memberName, sendMessageList, System.currentTimeMillis());
            }
        } catch (Exception e) {
            log.error("executeSchedulerTask error", e);
        } finally {
            redisUtil.releaseLock(String.format(CHAT_LOCK_KEY, memberName), memberName);
        }
    }

    private boolean checkNeedSendNewMsg(OpenAiApi.ChatCompletionMessage responseMessage, List<MemoryDTO> memoryDTOS, String memberName) {
        List<String> newMsgs = Optional.ofNullable(responseMessage.toolCalls()).orElse(new ArrayList<>()).stream().filter(tool -> tool.function().name().equals(REPLY_MESSAGE_FUNCTION_NAME)).flatMap(tool -> {
            SendMessageRequest sendMessageRequest = JSON.parseObject(tool.function().arguments(), SendMessageRequest.class);
            return Optional.ofNullable(sendMessageRequest.getSendTextMessageList()).stream().flatMap(Collection::stream);
        }).toList();
        if (CollectionUtils.isEmpty(newMsgs)) {
            return false;
        }
        String newMsg = StringUtils.join(newMsgs, "\n");
        StringBuilder memoryStr = getMemoryStrFromMemoryList(memoryDTOS);
        String response = springAiChat.generateJsonWithSingleMsgAndPrompt(promptFactory.getNewMsgCheckPrompt(memoryStr.toString(), memberName, newMsg));
        return Optional.ofNullable(JSON.parseObject(response).getBoolean("needSend")).orElse(false);
    }

    private String getNewsSchedulerPrompt(String memberName, boolean groupFlag, List<MemoryDTO> memoryDTOS, String news) {
        try {
            StringBuilder memoryStr = getMemoryStrFromMemoryList(memoryDTOS);
            return promptFactory.getChatPrompt(memberName, memoryStr.toString(), news, groupFlag, ChatSceneEnum.NEWS_SCHEDULE);
        } catch (Exception e) {
            log.error("getCheckStartMsgPrompt error", e);
        }
        return null;
    }

    @NotNull
    private static StringBuilder getMemoryStrFromMemoryList(List<MemoryDTO> memoryDTOS) {
        StringBuilder memoryStr = new StringBuilder();
//        Set<String> existSet = new HashSet<>();
        int index = 1;
        for (int i = 0; i < memoryDTOS.size(); i++) {
            MemoryDTO memorySingle = memoryDTOS.get(i);
//            if (existSet.contains(memorySingle.getMessageContent())) {
//                continue;
//            }
//            existSet.add(memorySingle.getMessageContent());
            memoryStr.append(index).append(". (").append(memorySingle.getMessageCreateAt()).append(")").append(Optional.ofNullable(memorySingle.getRealCreatorId()).orElse(Optional.ofNullable(memorySingle.getMessageCreatorId()).orElse(CreatorEnum.Andrew.getUserName()))).append(": ").append(memorySingle.getMessageContent()).append("\n");
            index++;
        }
        return memoryStr;
    }
}
