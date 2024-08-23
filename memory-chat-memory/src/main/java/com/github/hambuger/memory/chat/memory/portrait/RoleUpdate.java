package com.github.hambuger.memory.chat.memory.portrait;

import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.other.functionCall.aop.FunctionCallRegistry;
import com.github.hambuger.memory.chat.memory.other.util.RedisUtil;
import com.github.hambuger.memory.chat.memory.plan.DayPlanGenerate;
import com.github.hambuger.memory.chat.memory.portrait.model.SelfPortrait;

import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;


/**
 * @author hanjiabao
 * @since 2024/8/23
 */
@Slf4j
@Component
public class RoleUpdate {

    @Resource
    private SelfUpdate selfUpdate;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private DayPlanGenerate dayPlanGenerate;


    @FunctionCallRegistry(functionDesc = "新增角色的画像数据", scene = {ChatSceneEnum.ROLE_CHANGE})
    public Boolean addRole(SelfPortrait selfPortrait) {
        selfUpdate.updateSelfPortrait(selfPortrait);
        redisUtil.reset(DayPlanGenerate.DAY_PLAN_KEY);
        dayPlanGenerate.processPlanTasks();
        return true;
    }

}
