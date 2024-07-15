package com.github.hambuger.memory.chat.memory.plan;

import com.alibaba.fastjson.JSON;
import com.github.hambuger.memory.chat.memory.chat.dto.FriendPortrait;
import com.github.hambuger.memory.chat.memory.chat.dto.GroupPortrait;
import com.github.hambuger.memory.chat.memory.functionCall.aop.FunctionCallRegistry;
import com.github.hambuger.memory.chat.memory.util.RedisUtil;

import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;


/**
 * @author hanjiabao
 * @since 2024/7/15
 */
@Slf4j
@Component
public class PortraitUpdate {

    @Resource
    private RedisUtil redisUtil;


    @FunctionCallRegistry(scope = "GROUP")
    public boolean updateGroupPortrait(String groupName, GroupPortrait portrait) {
        redisUtil.updateGroupPortrait(groupName, JSON.toJSONString(portrait));
        return true;
    }


    @FunctionCallRegistry
    public boolean updateFriendPortrait(String friendName, FriendPortrait portrait) {
        redisUtil.updateFriendPortrait(friendName, JSON.toJSONString(portrait));
        return true;
    }

}
