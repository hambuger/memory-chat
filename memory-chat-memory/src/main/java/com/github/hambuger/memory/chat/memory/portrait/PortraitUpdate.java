package com.github.hambuger.memory.chat.memory.portrait;

import com.alibaba.fastjson.JSON;
import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.portrait.model.FriendPortrait;
import com.github.hambuger.memory.chat.memory.portrait.model.GroupPortrait;
import com.github.hambuger.memory.chat.memory.other.functionCall.aop.FunctionCallRegistry;
import com.github.hambuger.memory.chat.memory.other.util.RedisUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


/**
 * @author hanjiabao
 * @since 2024/7/15
 */
@Slf4j
@Component
public class PortraitUpdate {

    @Resource
    private RedisUtil redisUtil;



    @FunctionCallRegistry(functionDesc = "更新微信群的画像，可与回复消息并行执行", scene = {ChatSceneEnum.NORMAL_GROUP})
    public boolean updateGroupPortrait(GroupPortrait portrait) {
        redisUtil.updateGroupPortrait(portrait.getName(), JSON.toJSONString(portrait));
        return true;
    }


    @FunctionCallRegistry(functionDesc = "更新微信好友的画像，可与回复消息并行执行", scene = {ChatSceneEnum.NORMAL_USER})
    public boolean updateFriendPortrait(FriendPortrait portrait) {
        redisUtil.updateFriendPortrait(portrait.getName(), JSON.toJSONString(portrait));
        return true;
    }

}
