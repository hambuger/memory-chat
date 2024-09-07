package com.github.hambuger.memory.chat.memory.memory.reflection;

import com.drew.lang.StringUtil;
import com.github.hambuger.memory.chat.memory.chat.SpringAiChat;
import com.github.hambuger.memory.chat.memory.other.prompt.PromptFactory;
import com.github.hambuger.memory.chat.memory.other.util.RedisUtil;
import com.github.hambuger.memory.chat.memory.other.util.UserInfoUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.tika.utils.StringUtils;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class MindFlow {

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private PromptFactory promptFactory;

    @Resource
    private SpringAiChat springAiChat;

    public String getMindFlowKey(){
        return String.format("%s::mind_flow", UserInfoUtil.getUser());
    }

    public String getMindFlowFromMsg(String history){
        String midFlowPrompt = promptFactory.getMidFlowPrompt(history);
        List<OpenAiApi.ChatCompletionMessage> messageList = new ArrayList<>();
        messageList.add(new OpenAiApi.ChatCompletionMessage(midFlowPrompt, OpenAiApi.ChatCompletionMessage.Role.SYSTEM));
        OpenAiApi.ChatCompletion chatCompletion = springAiChat.generateMsgWithMsgList(messageList, false);
        return chatCompletion.choices().get(0).message().content();
    }

    public void generateMindFlowFromMsg(String messageId, String userName, String history) {
        String mind = getMindFlowFromMsg(history);
        String key;
        if (!StringUtils.isBlank(UserInfoUtil.getUser())) {
            key = getMindFlowKey();
        } else {
            key = String.format("%s::mind_flow", userName);
        }
        redisUtil.addElement(key, messageId + "::" + mind, 6);
    }

    public Map<String, String> getMindFlowMap() {
        Map<String, String> result = new HashMap<>();
        List<String> list = redisUtil.getList(getMindFlowKey());
        if (CollectionUtils.isEmpty(list)) {
            return result;
        }
        list.forEach(mind -> {
            List<String> stringList = Arrays.stream(mind.split("::")).toList();
            result.put(stringList.get(0), stringList.get(1));
        });
        return result;
    }

    public String getMindFlowByMsgId(String msgId) {
        return getMindFlowMap().get(msgId);
    }








}
