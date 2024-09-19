package com.github.hambuger.memory.chat.memory.chat;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Pair;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.hambuger.memory.chat.memory.audio.SpringAiAudio;
import com.github.hambuger.memory.chat.memory.chat.model.*;
import com.github.hambuger.memory.chat.memory.emoji.SogouEmoji;
import com.github.hambuger.memory.chat.memory.memory.create.MemoryInsert;
import com.github.hambuger.memory.chat.memory.memory.model.BaseMemoryDTO;
import com.github.hambuger.memory.chat.memory.memory.model.MemoryDTO;
import com.github.hambuger.memory.chat.memory.memory.model.SpringAiChatMessageMemoryDTO;
import com.github.hambuger.memory.chat.memory.memory.reflection.MindFlow;
import com.github.hambuger.memory.chat.memory.memory.search.MemorySearch;
import com.github.hambuger.memory.chat.memory.memory.update.MemoryUpdate;
import com.github.hambuger.memory.chat.memory.other.constants.CommonConstants;
import com.github.hambuger.memory.chat.memory.other.constants.MemoryChatConstants;
import com.github.hambuger.memory.chat.memory.other.functionCall.CallFunctionRegistryFactory;
import com.github.hambuger.memory.chat.memory.other.prompt.PromptFactory;
import com.github.hambuger.memory.chat.memory.other.token.TokenCalculation;
import com.github.hambuger.memory.chat.memory.other.util.*;
import com.github.hambuger.memory.chat.memory.portrait.PortraitGenerate;
import com.github.hambuger.memory.chat.memory.portrait.SelfUpdate;
import com.github.hambuger.memory.chat.memory.portrait.model.FriendPortrait;
import com.github.hambuger.memory.chat.memory.tools.docparse.DocParse;
import com.github.hambuger.memory.chat.memory.tools.weather.WeatherQuery;
import com.github.hambuger.memory.chat.memory.chat.message.SendMessage;
import com.github.hambuger.memory.chat.memory.chat.message.SendMessageRequest;
import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
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
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.github.hambuger.memory.chat.memory.other.constants.CommonConstants.*;
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

    @Value("${maxMsgCount}")
    private Integer maxMsgCount;

    @Autowired
    private SpringAiAudio springAiAudio;

    @Autowired
    private SpringAiChat springAiChat;

    @Autowired
    private VideoUtil videoUtil;

    @Resource
    private PicBedUtil picBedUtil;

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

    @Resource
    private SelfUpdate selfUpdate;

    @Resource
    private WeatherQuery weatherQuery;

    @Resource
    private PortraitGenerate portraitGenerate;

    @Resource
    private MindFlow mindFlow;

    @Resource
    private CommonMessageHandler commonMessageHandler;

    @Resource
    private FileUtil fileUtil;


    public static boolean checkLastMessageId(MemoryDTO memoryDTO) {
        String oldMsgId = LAST_MESSAGE_ID_MAP.get().get(memoryDTO.lastMsgIdMapKey());
        return StringUtils.isNotBlank(oldMsgId) && !StringUtils.equals(oldMsgId, StringUtils.equals(memoryDTO.getAiResponseFlag(), YES_STR) ? memoryDTO.getMessageParentIds().get(0) :
                memoryDTO.getMessageId());
    }


    public ChatResponse chat(BaseReceiveMessage baseMemoryDTO) {
        String lockKey = UUID.randomUUID().toString();
        try {
            log.info("get a new msg:{}", JSON.toJSONString(baseMemoryDTO));
            UserInfoUtil.putUser(baseMemoryDTO.getMessageCreatorName());
            if (checkCommandMessage(baseMemoryDTO)) {
                return getCommandResponse(baseMemoryDTO);
            }
            redisUtil.setString(baseMemoryDTO.chatLockKey(), lockKey);
            SpringAiChatMessageMemoryDTO memoryDTO = getChatMemory(baseMemoryDTO);
            String msgListKey = memoryDTO.msgCacheListKey();
            redisUtil.addMsg(msgListKey,
                    MemoryDTO.builder().messageId(memoryDTO.getMessageId()).messageCreateAt(memoryDTO.getMessageCreateAt()).realCreatorId(memoryDTO.getRealCreatorId()).messageCreatorId(memoryDTO.getMessageCreatorId()).groupMsgFlag(memoryDTO.getGroupMsgFlag()).messageContentType(memoryDTO.getMessageContentType()).aiResponseFlag(memoryDTO.getAiResponseFlag()).messageContent(memoryDTO.getMessageContent()).build());
            if (!memoryDTO.isDealFileFlag()) {
                // Asynchronously insert user messages
                CHAT_POOL.execute(() -> memoryInsert.insertNewMemory(memoryDTO, false));
            }
            // Update the last message id
            LAST_MESSAGE_ID_MAP.get().put(memoryDTO.lastMsgIdMapKey(), memoryDTO.getMessageId());
            // Check if this is the last message
            if (checkLastMessageId(memoryDTO)) {
                return null;
            }
            // Query the most relevant historical news
            List<MemoryDTO> searchMemoryList = StringUtils.equals(memoryDTO.getMessageContentType(), ContentTypeEnum.TEXT.getType()) ?
                    memorySearch.searchRelationMemory(memoryDTO.getMessageOwnerId(), memoryDTO.getMessageCreatorId(), memoryDTO.getMessageContent(), 0) : new ArrayList<>();

            List<MemoryDTO> memoryDTOS = redisUtil.getMsg(msgListKey);
            if (checkLastMessageId(memoryDTO)) {
                return null;
            }
            // Get recent chat history
            Pair<List<String>, LinkedList<OpenAiApi.ChatCompletionMessage>> listPair = getMinMemoryContext(msgListKey, memoryDTOS);
            List<String> existMsgIds = listPair.getKey();
            LinkedList<OpenAiApi.ChatCompletionMessage> messageList = listPair.getValue();
            if (checkLastMessageId(memoryDTO)) {
                return null;
            }
            OpenAiApi.ChatCompletionMessage systemMessage = getSystemMessage(memoryDTO, searchMemoryList, existMsgIds);
            messageList.add(systemMessage);
            if (checkLastMessageId(memoryDTO)) {
                return null;
            }
            // Get AI reply
            OpenAiApi.ChatCompletion aiMessageResponse = springAiChat.generateMsgWithMsgListAndFunctions(messageList, memoryDTO.groupFlag(), ChatSceneEnum.NORMAL_USER, 0.7d);
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
                chatMember.setGroupFlag(memoryDTO.groupFlag());
                chatMember.setSendUserId(baseMemoryDTO.getReceiveMessageUserId());
                chatMember.setChannelScene(baseMemoryDTO.getChannelEnum());
                redisUtil.addMember(chatMember);
            });
            // Convert to send message
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
            redisUtil.releaseLock(baseMemoryDTO.chatLockKey(), lockKey);
        }
    }


    private ChatResponse getCommandResponse(BaseMemoryDTO baseMemoryDTO) {
        String messageContent = baseMemoryDTO.getMessageContent().trim();
        List<SendMessage> sendMessageList = new ArrayList<>();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setMessageContentType(ContentTypeEnum.TEXT.getType());
        String replyContent;
        if (StringUtils.isBlank(messageContent)) {
            replyContent = promptFactory.getEmptySettingPrompt();
        }else {
            if (checkRoleContent(messageContent)) {
                replyContent = promptFactory.getResultSettingPrompt() + getRoleDetail(messageContent);
                CHAT_POOL.execute(() -> portraitGenerate.generateCustomChatModel(baseMemoryDTO.getMessageCreatorName(), messageContent));
            }else {
                replyContent = promptFactory.getErrorSettingPrompt();
            }
        }
        sendMessage.setMessageContent(replyContent);
        sendMessageList.add(sendMessage);
        return new ChatResponse(sendMessageList);
    }

    @Data
    public static class CheckRoleContentResult implements Serializable {

        @Serial
        private static final long serialVersionUID = -3058233413971990660L;

        @JsonPropertyDescription("is the content related to the character setting")
        @JsonProperty(required = true)
        private boolean relatedToRoleSetting;

        @JsonPropertyDescription("reason")
        @JsonProperty(required = true)
        private String reason;

    }


    private boolean checkRoleContent(String messageContent) {
        String json = """
{
    "relatedToRoleSetting": false,
    "reason": ""
}
""";
        String checkPrompt = promptFactory.getRoleContentCheckPrompt(messageContent, json);
        String aiResult = springAiChat.generateJsonWithSingleMsgAndPrompt(checkPrompt, CheckRoleContentResult.class);
        if (StringUtils.isBlank(aiResult)) {
            return false;
        }
        JSONObject jsonObject = JSONObject.parseObject(aiResult);
        return jsonObject.getBoolean("relatedToRoleSetting");
    }


    private String getRoleDetail(String content) {
        String rolePrompt = promptFactory.getRoleDetailPrompt(content);
        List<OpenAiApi.ChatCompletionMessage> messages = Lists.newArrayList(new OpenAiApi.ChatCompletionMessage(rolePrompt, OpenAiApi.ChatCompletionMessage.Role.SYSTEM));
        springAiChat.generateMsgWithMsgListAndFunctions(messages, false, ChatSceneEnum.ROLE_CHANGE, 0.7d);
        return selfUpdate.getSelfPortrait();
    }


    private boolean checkCommandMessage(BaseMemoryDTO baseMemoryDTO) {
        String messageContent = baseMemoryDTO.getMessageContent();
        if (StringUtils.isBlank(messageContent)) {
            return false;
        }
        String lowerStr = messageContent.trim().toLowerCase();
        if (lowerStr.startsWith("/change")) {
            baseMemoryDTO.setMessageContent(messageContent.replace("/change", "").trim());
            return true;
        }
        return false;
    }


    public void sendWxChatMessageList(String toUserId, String toUserName, List<SendMessage> sendMessageList, long receiveMsgTime) {
        String channelName = MessageChannelEnum.WECHAT.name();
        if (StringUtils.isBlank(toUserId)) {
            String userName = Optional.ofNullable(toUserName).orElse(UserInfoUtil.getUser());
            ChatMember member = redisUtil.getMember(userName);
            toUserId = member.getSendUserId();
            channelName = member.getChannelScene();
        }
        for (SendMessage sendMessage : sendMessageList) {
            SendChannelMessageRequest message = new SendChannelMessageRequest();
            message.setToUserId(toUserId);
            message.setChannelEnum(channelName);
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
            commonMessageHandler.sendMessage(message);
            receiveMsgTime = System.currentTimeMillis();
        }
    }


    private void startNewTaskForContact(MemoryDTO memoryDTO, String msgListKey) {
        StartConversationCheckTask.startTaskForContact(msgListKey, () -> {
            try {
                redisUtil.acquireLock(memoryDTO.chatLockKey(), memoryDTO.getMessageCreatorName(), 30 * 1000L, 60 * 1000L);
                List<MemoryDTO> memoryDTOS = redisUtil.getMsg(msgListKey);
                if (CollectionUtils.isEmpty(memoryDTOS)) {
                    return false;
                }
                boolean groupFlag = memoryDTO.groupFlag();
                String prompt = getCheckStartMsgPrompt(memoryDTO.getMessageCreatorName(), groupFlag, memoryDTOS);
                if (StringUtils.isBlank(prompt)) {
                    return false;
                }
                List<OpenAiApi.ChatCompletionMessage> messages = new ArrayList<>();
                messages.add(new OpenAiApi.ChatCompletionMessage(prompt, OpenAiApi.ChatCompletionMessage.Role.SYSTEM));
                String history = StringUtils.join(memoryDTOS.stream().map(msg -> Optional.ofNullable(msg.getRealCreatorName()).orElse(msg.getMessageCreatorName()) + ": " + msg.getMessageContent() + "(" + msg.getMessageCreateAt() + ")").collect(Collectors.toList()), "\n");
                history = history + promptFactory.getTimePrompt(memoryDTO.getMessageCreatorName(), formatDuration(DateUtil.between(DateUtil.parseDateTime(memoryDTOS.get(memoryDTOS.size() - 1).getMessageCreateAt()), new Date(), DateUnit.SECOND)));
                String mindFlowStr = mindFlow.getMindFlowFromMsg(history);
                messages.add(new OpenAiApi.ChatCompletionMessage(promptFactory.getTimePromptV2(memoryDTO.getMessageCreatorName(), formatDuration(DateUtil.between(DateUtil.parseDateTime(memoryDTOS.get(memoryDTOS.size() - 1).getMessageCreateAt()), new Date(), DateUnit.SECOND)), mindFlowStr), OpenAiApi.ChatCompletionMessage.Role.SYSTEM));
                addPerceptionMessage(memoryDTO.getMessageCreatorName(), messages);
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
                // Convert to send message
                List<SendMessage> sendMessageList = convertSendMessageList(responseMessage);
                if (CollectionUtils.isEmpty(sendMessageList)) {
                    return false;
                }else {
                    sendWxChatMessageList(null, memoryDTO.getMessageCreatorName(), sendMessageList, System.currentTimeMillis());
                }
                return true;
            } catch (Exception e) {
                log.error("startNewTaskForContact error", e);
                return false;
            } finally {
                redisUtil.releaseLock(memoryDTO.chatLockKey(), memoryDTO.getMessageCreatorName());
            }
        });
    }

    private void addPerceptionMessage(String name, List<OpenAiApi.ChatCompletionMessage> messages) {
        FriendPortrait portraitInfo = redisUtil.getFriendPortraitInfo(name);
        if (portraitInfo == null || StringUtils.isBlank(portraitInfo.getCity()) || StringUtils.equalsIgnoreCase(portraitInfo.getCity(), "Unknown")) {
            return;
        }
        WeatherQuery.WeatherParam param = new WeatherQuery.WeatherParam();
        param.setAddress(portraitInfo.getCity());
        String weather = weatherQuery.getWeather(param);
        if (StringUtils.isBlank(weather)) {
            return;
        }
        JSONObject weatherObject = JSON.parseObject(weather);
        String weatherStr = weatherObject.getString("weather");
        if (StringUtils.isBlank(weatherStr)) {
            return;
        }
        String beforeWeather = WeatherQuery.getBeforeWeather();
        if (StringUtils.isNotBlank(beforeWeather) && !StringUtils.equals(beforeWeather, weatherStr)) {
            OpenAiApi.ChatCompletionMessage newMsg = new OpenAiApi.ChatCompletionMessage(promptFactory.getWeatherChangePrompt(portraitInfo.getCity(), beforeWeather, weatherStr),
                    OpenAiApi.ChatCompletionMessage.Role.SYSTEM);
            messages.add(newMsg);
            WeatherQuery.setBeforeWeather(weatherStr);
        }
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
                                if (CollectionUtils.isNotEmpty(sendMessageRequest.getEmoticonPhotoUrlList())) {
                                    for (String emoji : sendMessageRequest.getEmoticonPhotoUrlList()) {
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
        boolean groupFlag = memoryDTO.groupFlag();
        // Select prompt
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
            memoryDTO.setMessageContentType(ContentTypeEnum.TEXT.getType());
            memoryDTO.setMessageContent(springAiAudio.generateTextWithAudio(new FileSystemResource(baseMemoryDTO.getMessageContent())));
        }else if (StringUtils.equals(baseMemoryDTO.getMessageContentType(), ContentTypeEnum.VIDEO.getType())) {
            memoryDTO.setMessageContentType(ContentTypeEnum.NOTE.getType());
            memoryDTO.setMessageContent(String.format("%s sent you a video, currently viewing it", Optional.ofNullable(baseMemoryDTO.getRealCreatorName()).orElse(baseMemoryDTO.getMessageCreatorName())));
        }else if(StringUtils.equals(baseMemoryDTO.getMessageContentType(), ContentTypeEnum.PICTURE.getType()) || StringUtils.equals(baseMemoryDTO.getMessageContentType(), ContentTypeEnum.EMOJI.getType())){
            memoryDTO.setMessageContent(picBedUtil.uploadImage(baseMemoryDTO.getMessageContent()));
        }else if(StringUtils.equals(baseMemoryDTO.getMessageContentType(), ContentTypeEnum.APP.getType())) {
            memoryDTO.setMessageContentType(ContentTypeEnum.NOTE.getType());
            memoryDTO.setMessageContent(String.format("%s sent you a file, file path: %s, currently viewing", Optional.ofNullable(baseMemoryDTO.getRealCreatorName()).orElse(baseMemoryDTO.getMessageCreatorName()), baseMemoryDTO.getMessageContent()));
        }
        memoryDTO.setMessageCreatorId(memoryDTO.getMessageCreatorName());
        memoryDTO.setMessageCreatorType(baseMemoryDTO.groupFlag() ? CreatorEnum.GROUP.getType() : CreatorEnum.USER.getType());
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
                redisUtil.updateMsgContentById(memoryDTO.msgCacheListKey(), memoryDTO.getMessageId(), fileDesc);
            });
        }
        return memoryDTO;
    }


    private String getFileInfo(BaseMemoryDTO baseMemoryDTO) {
        StringBuilder info = new StringBuilder();
        info.append(Optional.ofNullable(baseMemoryDTO.getRealCreatorName()).orElse(baseMemoryDTO.getMessageCreatorName()));
        info.append(promptFactory.getFileSendPrompt());
        info.append(baseMemoryDTO.getMessageContent()).append("\n");
        info.append(promptFactory.getFileSendPromptV2());
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
                                if (CollectionUtils.isNotEmpty(sendMessageRequest.getEmoticonPhotoUrlList())) {
                                    sendMessageRequest.getEmoticonPhotoUrlList().stream().forEach(emoji -> sendMessageList.add(new SendMessage(emoji,
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
        messageList.add(new OpenAiApi.ChatCompletionMessage("Now there is a video subtitle information and a collection of screenshots from the video. You need to give a detailed description of the video so that others can understand the content of the video through this description. The reply only needs to give a description, without other redundant information.\n", OpenAiApi.ChatCompletionMessage.Role.SYSTEM));
        StringBuilder prompt = new StringBuilder();
        if (StringUtils.isNotBlank(audioText)) {
            prompt.append(String.format("The subtitle information of the video is as follows：%s", audioText));
        }
        if (CollectionUtils.isNotEmpty(imageList)) {
            prompt.append("The screenshots of the video are as follows");
        }
        contentList.add(new OpenAiApi.ChatCompletionMessage.MediaContent(prompt.toString()));
        if (CollectionUtils.isNotEmpty(imageList)) {
            List<String> uploadedImageList = picBedUtil.uploadImages(imageList);
            for (String image : uploadedImageList) {
                contentList.add(new OpenAiApi.ChatCompletionMessage.MediaContent(new OpenAiApi.ChatCompletionMessage.MediaContent.ImageUrl(image, "low")));
            }
        }
        messageList.add(new OpenAiApi.ChatCompletionMessage(contentList, OpenAiApi.ChatCompletionMessage.Role.USER));
        OpenAiApi.ChatCompletion response = springAiChat.generateMsgWithMsgList(messageList, false);
        String videoInfo = response.choices().get(0).message().content();
        return promptFactory.getVideoInfoPrompt(creatorName, videoInfo);
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
                String mind = mindFlow.getMindFlowByMsgId(memoryDTO.getMessageId());
                String content = memoryDTO.getMessageContent();
                if (StringUtils.isNotBlank(mind)) {
                    content = content + promptFactory.getThoughtPrompt(mind);
                }
                chatMessage = new OpenAiApi.ChatCompletionMessage(content, OpenAiApi.ChatCompletionMessage.Role.ASSISTANT);
                sumMsgToken = sumMsgToken + tokenCalculation.getUserMessageToken(chatMessage);
            }else {
                chatMessage = convertMemoryMsg2SpringAiModelMsg(memoryDTO);
                sumMsgToken = sumMsgToken + tokenCalculation.getUserMessageToken(chatMessage);
            }
            msgCount++;
            if (sumMsgToken < maxMsgToken && msgCount <= maxMsgCount) {
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
            return seconds + "Seconds";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            return minutes + "Minutes";
        } else if (seconds < 86400) {
            long hours = seconds / 3600;
            return hours + "Hours";
        } else {
            long days = seconds / 86400;
            return days + "Days";
        }
    }

    public void executeSchedulerTask(ChatMember chatMember, String news) {

        String memberName = chatMember.getName();
        boolean groupFlag = chatMember.isGroupFlag();
        String msgListKey = memberName + MemoryChatConstants.MSG_LIST_KEY_SUFFIX;
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
            messages.add(new OpenAiApi.ChatCompletionMessage(promptFactory.getTimePrompt(chatMember.getName(), formatDuration(DateUtil.between(DateUtil.parseDateTime(memoryDTOS.get(memoryDTOS.size() - 1).getMessageCreateAt()), new Date(), DateUnit.SECOND))), OpenAiApi.ChatCompletionMessage.Role.SYSTEM));
            addPerceptionMessage(chatMember.getName(), messages);
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
            // Convert to send message
            List<SendMessage> sendMessageList = convertSendMessageList(responseMessage);
            if (CollectionUtils.isNotEmpty(sendMessageList)) {
                sendWxChatMessageList(null, memberName, sendMessageList, System.currentTimeMillis());
            }
        } catch (Exception e) {
            log.error("executeSchedulerTask error", e);
        } finally {
            redisUtil.releaseLock(String.format(CHAT_LOCK_KEY, memberName), memberName);
        }
    }

    public static class NeedSendResult implements Serializable {

        @Serial
        private static final long serialVersionUID = 543081299572723565L;

        @JsonPropertyDescription("Do you need to send")
        @JsonProperty(required = true)
        private boolean needSend;

        @JsonPropertyDescription("reason")
        @JsonProperty(required = true)
        private String reason;

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
        String response = springAiChat.generateJsonWithSingleMsgAndPrompt(promptFactory.getNewMsgCheckPrompt(memoryStr.toString(), memberName, newMsg), NeedSendResult.class);
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
        int index = 1;
        for (int i = 0; i < memoryDTOS.size(); i++) {
            MemoryDTO memorySingle = memoryDTOS.get(i);
            memoryStr.append(index).append(". (").append(memorySingle.getMessageCreateAt()).append(")").append(Optional.ofNullable(memorySingle.getRealCreatorId()).orElse(Optional.ofNullable(memorySingle.getMessageCreatorId()).orElse(CreatorEnum.Andrew.getUserName()))).append(": ").append(memorySingle.getMessageContent()).append("\n");
            index++;
        }
        return memoryStr;
    }
}
