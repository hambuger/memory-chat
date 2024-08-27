package com.github.hambuger.memory.chat.memory.portrait;

import com.alibaba.fastjson.JSON;
import com.github.hambuger.memory.chat.memory.chat.SpringAiChat;
import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.other.prompt.PromptFactory;
import com.github.hambuger.memory.chat.memory.portrait.model.FriendPortrait;
import com.github.hambuger.memory.chat.memory.portrait.model.GroupPortrait;
import com.github.hambuger.memory.chat.memory.other.functionCall.aop.FunctionCallRegistry;
import com.github.hambuger.memory.chat.memory.other.util.RedisUtil;

import cn.hutool.core.date.DateUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


/**
 * @author hanjiabao
 * @since 2024/7/15
 */
@Slf4j
@Component
public class PortraitUpdate {

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private PromptFactory promptFactory;

    @Resource
    private SpringAiChat springAiChat;



    @FunctionCallRegistry(functionDesc = "更新微信群的画像，可与回复消息并行执行", scene = {ChatSceneEnum.NORMAL_GROUP})
    public Boolean updateGroupPortrait(GroupPortrait portrait) {
        redisUtil.updateGroupPortrait(portrait.getName(), JSON.toJSONString(portrait));
        return true;
    }

    @FunctionCallRegistry(functionDesc = "更新好友的画像", scene = {ChatSceneEnum.UPDATE_FRIEND_PORTRAIT})
    public Boolean updateFriendPortraitInfo(FriendPortrait portrait) {
        if (StringUtils.isBlank(portrait.getName())) {
            return true;
        }
        redisUtil.updateFriendPortrait(portrait.getName(), JSON.toJSONString(portrait));
        return true;
    }


    @FunctionCallRegistry(functionDesc = "更新微信好友的画像，可与回复消息并行执行", scene = {ChatSceneEnum.NORMAL_USER})
    public Boolean updateFriendPortrait(FriendPortrait portrait) {
        if (StringUtils.isNotBlank(portrait.getDoing()) && !StringUtils.equals(portrait.getDoing(), "Unknown") && !portrait.getDoing().contains("(")) {
            portrait.setDoing(portrait.getDoing() + "(" + DateUtil.now() + ")");
        }
        String friendPortrait = redisUtil.getFriendPortrait(portrait.getName());
        if (StringUtils.isNotBlank(friendPortrait)) {
            String beforePortrait = friendPortrait.replace("FriendPortrait", "BeforeFriendPortrait");
            String afterPortrait = portrait.toMarkDown().replace("FriendPortrait", "AfterFriendPortrait");
            String prompt = promptFactory.getFriendPortraitUpdatePrompt(portrait.getName(), beforePortrait, afterPortrait);
            List<OpenAiApi.ChatCompletionMessage> messages = new ArrayList<>();
            messages.add(new OpenAiApi.ChatCompletionMessage(prompt, OpenAiApi.ChatCompletionMessage.Role.SYSTEM));
            springAiChat.generateMsgWithMsgListAndFunctions(messages, false, ChatSceneEnum.UPDATE_FRIEND_PORTRAIT);
        } else {
            redisUtil.updateFriendPortrait(portrait.getName(), JSON.toJSONString(portrait));
        }
        return true;
    }

}
