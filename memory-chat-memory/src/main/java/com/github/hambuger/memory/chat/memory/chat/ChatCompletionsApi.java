package com.github.hambuger.memory.chat.memory.chat;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Pair;
import com.alibaba.fastjson.JSON;
import com.github.hambuger.memory.chat.memory.audio.SpringAiAudio;
import com.github.hambuger.memory.chat.memory.chat.dto.*;
import com.github.hambuger.memory.chat.memory.constants.CommonConstants;
import com.github.hambuger.memory.chat.memory.constants.Constants;
import com.github.hambuger.memory.chat.memory.memory.MemoryInsert;
import com.github.hambuger.memory.chat.memory.memory.MemorySearch;
import com.github.hambuger.memory.chat.memory.memory.MemoryUpdate;
import com.github.hambuger.memory.chat.memory.memory.model.BaseMemoryDTO;
import com.github.hambuger.memory.chat.memory.memory.model.MemoryDTO;
import com.github.hambuger.memory.chat.memory.token.TokenCalculation;
import com.github.hambuger.memory.chat.memory.util.*;
import com.github.hambuger.memory.chat.memory.wechat.SendMessage;
import com.github.hambuger.memory.chat.memory.wechat.SendMessageRequest;
import com.github.hambuger.memory.chat.wechat.api.DownloadTools;
import com.github.hambuger.memory.chat.wechat.api.MessageTools;
import com.github.hambuger.memory.chat.wechat.entity.Message;
import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.hambuger.memory.chat.memory.constants.CommonConstants.DOUBLE_COLON;
import static com.github.hambuger.memory.chat.memory.constants.CommonConstants.YES_STR;
import static com.github.hambuger.memory.chat.memory.constants.Constants.*;
import static java.lang.String.format;


/**
 * @author hamburger
 * @since 2024/6/3
 */
@Slf4j
@Component
public class ChatCompletionsApi {

    private static final ThreadPoolExecutor CHAT_POOL = new ThreadPoolExecutor(10, 20, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000), new CustomizableThreadFactory("chat-pool"), new ThreadPoolExecutor.CallerRunsPolicy());


    private static final AtomicReference<ConcurrentHashMap<String, String>> LAST_MESSAGE_ID_MAP = new AtomicReference<>(new ConcurrentHashMap());

    @Autowired
    private SpringAiAudio springAiAudio;

    @Autowired
    private SpringAiChat springAiChat;

    @Autowired
    private VideoUtil videoUtil;

    @Resource
    private ImageUploadUtils imageUploadUtils;


    public static boolean checkLastMessageId(MemoryDTO memoryDTO) {
        String lastMsgIdMapKey = memoryDTO.getMessageOwnerId() + DOUBLE_COLON + (StringUtils.equals(memoryDTO.getAiResponseFlag(), YES_STR) ? memoryDTO.getMessageReceiveId() : memoryDTO.getMessageCreatorId());
        String oldMsgId = LAST_MESSAGE_ID_MAP.get().get(lastMsgIdMapKey);
        return StringUtils.isNotBlank(oldMsgId) && !StringUtils.equals(oldMsgId, StringUtils.equals(memoryDTO.getAiResponseFlag(), YES_STR) ? memoryDTO.getMessageParentIds().get(0) : memoryDTO.getMessageId());
    }


    public ChatResponse chat(ExtraBaseMemoryDTO baseMemoryDTO) {
        try {
            log.info("get a new msg:{}", JSON.toJSONString(baseMemoryDTO));
            SpringAiChatMessageMemoryDTO memoryDTO = getChatMemory(baseMemoryDTO);
            String lastMsgIdMapKey = memoryDTO.getMessageOwnerId() + DOUBLE_COLON + memoryDTO.getMessageCreatorId();
            String msgListKey = memoryDTO.getMessageOwnerId() + DOUBLE_COLON + memoryDTO.getMessageCreatorId() + Constants.MSG_LIST_KEY_SUFFIX;
            RedisLikeCounter.addMsg(msgListKey, MemoryDTO.builder().messageId(memoryDTO.getMessageId()).messageCreateAt(memoryDTO.getMessageCreateAt()).realCreatorId(memoryDTO.getRealCreatorId()).messageCreatorId(memoryDTO.getMessageCreatorId()).groupMsgFlag(memoryDTO.getGroupMsgFlag()).messageContentType(memoryDTO.getMessageContentType()).aiResponseFlag(memoryDTO.getAiResponseFlag()).messageContent(memoryDTO.getMessageContent()).build());
            // 异步插入用户消息
            CHAT_POOL.execute(() -> MemoryInsert.insertNewMemory(memoryDTO));
            // 更新最后一条消息id
            LAST_MESSAGE_ID_MAP.get().put(lastMsgIdMapKey, memoryDTO.getMessageId());
            // 检查是否是最后一条消息
            if (checkLastMessageId(memoryDTO)) {
                return null;
            }
            // 查询相关性最高的历史消息
            List<MemoryDTO> searchMemoryList = StringUtils.equals(memoryDTO.getMessageContentType(), ContentTypeEnum.TEXT.getType()) ? MemorySearch.searchRelationMemory(memoryDTO.getMessageOwnerId(), memoryDTO.getMessageCreatorId(), memoryDTO.getMessageContent()) : new ArrayList<>();

            List<MemoryDTO> memoryDTOS = RedisLikeCounter.getMsg(msgListKey);
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
            OpenAiApi.ChatCompletion aiMessageResponse = springAiChat.generateMsgWithMsgListAndFunctions(messageList);
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
            if (CollectionUtils.isEmpty(memoryDTOS)) {
                return false;
            }
            String prompt = getCheckStartMsgPrompt(memoryDTO.getMessageCreatorName(), memoryDTOS);
            if (StringUtils.isBlank(prompt)) {
                return false;
            }
            String aiResponse = LangChainChat.generateJsonWithSingleMsgAndPrompt(prompt);
            if (StringUtils.isBlank(aiResponse)) {
                return false;
            }
            SendMessageRequest sendMessageRequest = JSON.parseObject(aiResponse, SendMessageRequest.class);
            if (sendMessageRequest == null || !sendMessageRequest.isNeedsSending() || (CollectionUtils.isEmpty(sendMessageRequest.getSendPictureMessageList()) && CollectionUtils.isEmpty(sendMessageRequest.getSendTextMessageList()))) {
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
                    CHAT_POOL.execute(() -> MemoryInsert.insertNewMemory(aiMemoryDTO));
                }
            } else {
                return false;
            }
            MessageTools.sendMsgByUserId(sendMessageList);
            return true;
        });
    }

    private String getCheckStartMsgPrompt(String toUserName, List<MemoryDTO> memoryDTOS) {
        try {
            StringBuilder promptBuilder = new StringBuilder().append("你是Andrew,下面是你和%s的离当前时间最近的对话内容。【%s】\n判断一下是否需要给%s发送一个新的消息。\n").append("应该是在确实有必要的情况下才发起会话,尽量不要打扰别人，尤其是在夜晚时间。长时间没有联系的人也要谨慎判断是否发起消息。\n").append("即使需要发送消息，也不要频繁发送重复的信息\n").append("如果需要发送消息，给出需要发送的消息内容。你给出的发送消息的判断和消息内容应该类似如下的json格式：\n %s\n").append("其中needsSending字段表示是否需要发送消息，sendTextMessageList字段表示需要发送的消息内容\n").append("注意：现在时间是%s");
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
            } else {
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


    private static @NotNull OpenAiApi.ChatCompletionMessage getSystemMessage(MemoryDTO memoryDTO, List<MemoryDTO> searchMemoryList, List<String> existMsgIds) {
        OpenAiApi.ChatCompletionMessage systemMessage;
        boolean groupFlag = StringUtils.equals(memoryDTO.getGroupMsgFlag(), YES_STR);
        String now = DateUtil.format(new Date(), DatePattern.NORM_DATETIME_FORMAT);
        // 选择prompt
        if (CollectionUtils.isEmpty(searchMemoryList)) {
            systemMessage = new OpenAiApi.ChatCompletionMessage(String.format(groupFlag ? Constants.GROUP_PROMPT_PREFIX : Constants.PROMPT_PREFIX, memoryDTO.getMessageCreatorName()) + String.format(groupFlag ? Constants.GROUP_PROMPT_END : Constants.PROMPT_END, now), OpenAiApi.ChatCompletionMessage.Role.SYSTEM);
        } else {
            StringBuilder memory = new StringBuilder();
            for (int i = 1; i < searchMemoryList.size(); i++) {
                MemoryDTO memorySingle = searchMemoryList.get(i);
                if (existMsgIds.contains(memorySingle.getMessageId())) {
                    continue;
                }
                memory.append(i).append("(").append(memorySingle.getMessageCreateAt()).append(")").append(Optional.ofNullable(memorySingle.getRealCreatorName()).orElse(memorySingle.getMessageCreatorName())).append(":").append(memorySingle.getMessageContent()).append("\n");
                MemoryUpdate.updateMemoryAccessTime(memorySingle.getMessageId());
            }
            systemMessage = new OpenAiApi.ChatCompletionMessage(String.format(groupFlag ? Constants.GROUP_PROMPT_PREFIX : Constants.PROMPT_PREFIX, memoryDTO.getMessageCreatorName()) + (StringUtils.isNotBlank(memory) ? String.format(groupFlag ? Constants.GROUP_PROMPT_MID : Constants.PROMPT_MID, memory) : "") + String.format(groupFlag ? Constants.GROUP_PROMPT_END : Constants.PROMPT_END, now), OpenAiApi.ChatCompletionMessage.Role.SYSTEM);
        }
        return systemMessage;
    }


    private SpringAiChatMessageMemoryDTO getChatMemory(BaseMemoryDTO baseMemoryDTO) {
        if (StringUtils.equals(baseMemoryDTO.getMessageContentType(), ContentTypeEnum.AUDIO.getType())) {
            DownloadTools.awaitDownload(baseMemoryDTO.getMessageContent());
            baseMemoryDTO.setMessageContentType(ContentTypeEnum.TEXT.getType());
            baseMemoryDTO.setMessageContent(springAiAudio.generateTextWithAudio(new FileSystemResource(baseMemoryDTO.getMessageContent())));
        }else if (StringUtils.equals(baseMemoryDTO.getMessageContentType(), ContentTypeEnum.VIDEO.getType())) {
            DownloadTools.awaitDownload(baseMemoryDTO.getMessageContent());
            baseMemoryDTO.setMessageContentType(ContentTypeEnum.NOTE.getType());
            baseMemoryDTO.setMessageContent(getVideoInfo(baseMemoryDTO));
        }
        SpringAiChatMessageMemoryDTO memoryDTO = BeanUtil.copyProperties(baseMemoryDTO, SpringAiChatMessageMemoryDTO.class);
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
        OpenAiApi.ChatCompletionMessage message = convertMemoryMsg2SpringAiModelMsg(memoryDTO);
        memoryDTO.setChatMessage(message);
        memoryDTO.setUseToken(new TokenCalculation(Constants.MODEL_NAME).getUserMessageToken(message));
        return memoryDTO;
    }


    private List<MemoryDTO> convertSpringMsg2AiMSg(OpenAiApi.ChatCompletionMessage aiMessage, MemoryDTO memoryDTO, Integer token) {
        List<MemoryDTO> memoryDTOS = new ArrayList<>();
        if (aiMessage != null) {
            if (CollectionUtils.isEmpty(aiMessage.toolCalls())) {
                MemoryDTO aiMemoryDTO = getAiResponseMemoryDTO(memoryDTO, ContentTypeEnum.TEXT.getType(), aiMessage.content(), token);
                memoryDTOS.add(aiMemoryDTO);
            } else {
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


    public OpenAiApi.ChatCompletionMessage convertMemoryMsg2SpringAiModelMsg(BaseMemoryDTO memoryDTO) {
        OpenAiApi.ChatCompletionMessage message = null;
        ContentTypeEnum contentTypeEnum = ContentTypeEnum.getByType(memoryDTO.getMessageContentType());
        switch (contentTypeEnum){
            case NOTE:
                message = new OpenAiApi.ChatCompletionMessage(memoryDTO.getMessageContent(), OpenAiApi.ChatCompletionMessage.Role.SYSTEM);
                break;
            case TEXT:
                message = new OpenAiApi.ChatCompletionMessage(memoryDTO.getMessageContent(), OpenAiApi.ChatCompletionMessage.Role.USER);
                break;
            case PICTURE:
                message = new OpenAiApi.ChatCompletionMessage(Lists.newArrayList(new OpenAiApi.ChatCompletionMessage.MediaContent(new OpenAiApi.ChatCompletionMessage.MediaContent.ImageUrl(format("data:%s;base64,%s", IMAGE_TYPE, FileUtil.getFileBase64Data(memoryDTO.getMessageContent(), true)), "auto"))), OpenAiApi.ChatCompletionMessage.Role.USER);
                break;
            case EMOJI:
                message = new OpenAiApi.ChatCompletionMessage(Lists.newArrayList(new OpenAiApi.ChatCompletionMessage.MediaContent(new OpenAiApi.ChatCompletionMessage.MediaContent.ImageUrl(format("data:%s;base64,%s", EMOJI_TYPE, FileUtil.getFileBase64Data(memoryDTO.getMessageContent(), true)), "auto"))), OpenAiApi.ChatCompletionMessage.Role.USER);
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
            return getVideInfoText(memoryDTO.getMessageCreatorName(), imageList, audioPathAntText.get(1));
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
        OpenAiApi.ChatCompletion response = springAiChat.generateMsgWithMsgList(messageList);
        String videoInfo = response.choices().get(0).message().content();
        return String.format("%s发送了一个视频。这个视频的信息如下：%s", creatorName, videoInfo);
    }


    private Pair<List<String>, LinkedList<OpenAiApi.ChatCompletionMessage>> getMinMemoryContext(String msgListKey, List<MemoryDTO> memoryDTOS) {
        if (CollectionUtils.isEmpty(memoryDTOS)) {
            return new Pair<>(new ArrayList<>(), new LinkedList<>());
        }
        LinkedList<OpenAiApi.ChatCompletionMessage> messageList = new LinkedList<>();
        List<String> existMsgIdList = new ArrayList<>();
        int sumMsgToken = 0;
        for (int i = memoryDTOS.size() - 1; i >= 0; i--) {
            MemoryDTO memoryDTO = memoryDTOS.get(i);
            OpenAiApi.ChatCompletionMessage chatMessage;
            if (StringUtils.isBlank(memoryDTO.getMessageContentType()) || StringUtils.isBlank(memoryDTO.getMessageContent())) {
                continue;
            }
            if (memoryDTO.getAiResponseFlag().equals(YES_STR)) {
                chatMessage = new OpenAiApi.ChatCompletionMessage(memoryDTO.getMessageContent(), OpenAiApi.ChatCompletionMessage.Role.ASSISTANT);
                sumMsgToken = sumMsgToken + new TokenCalculation(Constants.MODEL_NAME).getUserMessageToken(chatMessage);
            } else {
                chatMessage = convertMemoryMsg2SpringAiModelMsg(memoryDTO);
                sumMsgToken = sumMsgToken + new TokenCalculation(Constants.MODEL_NAME).getUserMessageToken(chatMessage);
            }
            if (sumMsgToken < Constants.MAX_MSG_TOKEN) {
                messageList.addFirst(chatMessage);
                existMsgIdList.add(memoryDTO.getMessageId());
            } else {
                delOldMessageFromCache(msgListKey, i);
                break;
            }

        }
        return new Pair<>(existMsgIdList, messageList);
    }


    private static void delOldMessageFromCache(String msgListKey, int i) {
        RedisLikeCounter.delOldMemory(msgListKey, i);
    }

}
