package com.github.hambuger.memory.chat.memory.memory.reflection;

import com.alibaba.fastjson.JSON;
import com.github.hambuger.memory.chat.memory.chat.SpringAiChat;
import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.memory.create.MemoryInsert;
import com.github.hambuger.memory.chat.memory.memory.model.MemoryDTO;
import com.github.hambuger.memory.chat.memory.other.functionCall.aop.FunctionCallRegistry;
import com.github.hambuger.memory.chat.memory.other.prompt.PromptFactory;
import com.github.hambuger.memory.chat.memory.other.util.EsClient;
import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.hambuger.memory.chat.memory.other.constants.CommonConstants.YES_STR;


/**
 * @author hamburger
 * @since 2024/8/9
 */
@Slf4j
@Component
public class MemoryMergeTask {

    @Resource
    private EsClient esClient;

    @Resource
    private MemoryInsert memoryInsert;

    @Resource
    private PromptFactory promptFactory;

    @Resource
    private SpringAiChat springAiChat;

    @Value("${chatMemoryIndex}")
    private String chatMemoryIndex;

    private static final ConcurrentHashMap<String, MemoryDTO> memoryContentMap = new ConcurrentHashMap<>();

    public void memoryMerge(MemoryDTO memoryDTO, String historyMemory) {
        if (memoryDTO == null || StringUtils.isBlank(historyMemory)) {
            return;
        }
        String mergePrompt = promptFactory.getMemoryMergePrompt(memoryDTO.getMessageContent(), historyMemory);
        List<OpenAiApi.ChatCompletionMessage> messages = Lists.newArrayList(new OpenAiApi.ChatCompletionMessage(mergePrompt, OpenAiApi.ChatCompletionMessage.Role.SYSTEM));
        memoryContentMap.put(memoryDTO.getMessageContent(), memoryDTO);
        springAiChat.generateMsgWithMsgListAndFunctions(messages, false, ChatSceneEnum.MEMORY_MERGE);
    }


    @FunctionCallRegistry(functionDesc = "Add a new memory", scene = {ChatSceneEnum.MEMORY_MERGE})
    public Boolean insertNewMemory(String content) {
        MemoryDTO memoryDTO = memoryContentMap.get(content);
        if (memoryDTO == null) {
            return true;
        }
        memoryDTO.setMessageContent(content);
        memoryInsert.insertNewMemory(memoryDTO, true);
        memoryContentMap.remove(content);
        return true;
    }

    @FunctionCallRegistry(functionDesc = "Update the original memory content", scene = {ChatSceneEnum.MEMORY_MERGE})
    public Boolean updateOldMemory(String messageId, String content) {
        try {
            if (StringUtils.isAnyBlank(messageId, content)) {
                return true;
            }
            UpdateByQueryRequest updateByQueryRequest = new UpdateByQueryRequest(chatMemoryIndex);
            updateByQueryRequest.setQuery(QueryBuilders.termQuery("messageId", messageId));
            Map<String, Object> params = new HashMap<>();
            params.put("messageContent", content);
            Script inline = new Script(ScriptType.INLINE, "painless", "ctx._source.messageContent = params.messageContent", params);
            updateByQueryRequest.setScript(inline);
            esClient.updateByQuery(updateByQueryRequest);
        } catch (IOException e) {
            log.warn("updateOldMemory error", e);
        }
        return true;
    }

    @FunctionCallRegistry(functionDesc = "delete original memory", scene = {ChatSceneEnum.MEMORY_MERGE})
    public Boolean deleteOldMemory(String messageId) {
        MemoryDTO memoryDTO = new MemoryDTO();
        memoryDTO.setIsDeleted(YES_STR);
        UpdateRequest updateRequest = new UpdateRequest(chatMemoryIndex, messageId).doc(JSON.toJSONString(memoryDTO), XContentType.JSON);
        try {
            esClient.update(updateRequest);
        } catch (IOException e) {
            log.warn("deleteOldMemory error", e);
        }
        return true;
    }


}
