package com.github.hambuger.memory.chat.memory.portrait;

import com.alibaba.fastjson.JSON;
import com.github.hambuger.memory.chat.memory.chat.SpringAiChat;
import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.other.functionCall.aop.FunctionCallRegistry;
import com.github.hambuger.memory.chat.memory.other.prompt.PromptFactory;
import com.github.hambuger.memory.chat.memory.other.util.RedisUtil;
import com.github.hambuger.memory.chat.memory.plan.DayPlanGenerate;
import com.github.hambuger.memory.chat.memory.portrait.model.SelfPortrait;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.util.*;

import cn.hutool.core.date.DateUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import static com.github.hambuger.memory.chat.memory.other.constants.MemoryChatConstants.SELF_PORTRAIT_KEY;


@Slf4j
@Component
public class SelfUpdate {

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private PromptFactory promptFactory;

    @Resource
    private SpringAiChat springAiChat;

    @Resource
    private DayPlanGenerate dayPlanGenerate;

    @FunctionCallRegistry(functionDesc = "更新画像数据", scene = {ChatSceneEnum.UPDATE_SELF_PORTRAIT})
    public Boolean updatePortraitInfo(SelfPortrait param) {
        redisUtil.setString(SELF_PORTRAIT_KEY, JSON.toJSONString(param));
        return true;
    }

    @FunctionCallRegistry(functionDesc = "新增角色的画像数据", scene = {ChatSceneEnum.ROLE_CHANGE})
    public Boolean addRole(SelfPortrait selfPortrait) {
        updateSelfPortrait(selfPortrait);
        redisUtil.reset(DayPlanGenerate.DAY_PLAN_KEY);
        dayPlanGenerate.processPlanTasks();
        return true;
    }

    @FunctionCallRegistry(functionDesc = "更新Andrew的自我画像，可与回复消息并行执行", scene = {ChatSceneEnum.NORMAL_USER, ChatSceneEnum.NORMAL_GROUP})
    public Boolean updateSelfPortrait(SelfPortrait param) {
        String selfPortrait = redisUtil.getString(SELF_PORTRAIT_KEY);
        if (StringUtils.isNotBlank(selfPortrait)) {
            SelfPortrait selfPortraitObj = JSON.parseObject(selfPortrait, SelfPortrait.class);
            String beforePortrait = selfPortraitObj.toMarkDown();
            String afterPortrait = param.toMarkDown();
            String prompt = promptFactory.getSelfPortraitUpdatePrompt(beforePortrait, afterPortrait);
            List<OpenAiApi.ChatCompletionMessage> messages = new ArrayList<>();
            messages.add(new OpenAiApi.ChatCompletionMessage(prompt, OpenAiApi.ChatCompletionMessage.Role.SYSTEM));
            springAiChat.generateMsgWithMsgListAndFunctions(messages, false, ChatSceneEnum.UPDATE_SELF_PORTRAIT);
        } else {
            redisUtil.setString(SELF_PORTRAIT_KEY, JSON.toJSONString(param));
        }
        return true;
    }


    public String getSelfPortrait() {
        String portraitStr = redisUtil.getString(SELF_PORTRAIT_KEY);
        Map<Object, Object> hourPlanMap = redisUtil.getMap(DayPlanGenerate.DAY_PLAN_KEY);
        int nowHour = DateUtil.thisHour(true);
        String doing = Optional.ofNullable(hourPlanMap).map(map -> {
            Object object = map.get(Integer.toString(nowHour));
            if (object == null) {
                return "Unknown";
            }
            StringBuilder info = new StringBuilder(object.toString());
            info.append("(");
            info.append("已进行了");
            int minute = DateUtil.minute(new Date());
            info.append(minute);
            info.append("分钟, 还剩下");
            info.append(60 - minute);
            info.append("分钟)");
            return info.toString();
        }).orElse("Unknown");
        if (StringUtils.isBlank(portraitStr)) {
            return null;
        }else {
            SelfPortrait selfPortrait = JSON.parseObject(portraitStr, SelfPortrait.class);
            return selfPortrait.toMarkDown() + String.format("- Doing: %s\n", doing);
        }
    }

}
