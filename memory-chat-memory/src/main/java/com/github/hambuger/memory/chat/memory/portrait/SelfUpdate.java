package com.github.hambuger.memory.chat.memory.portrait;

import com.alibaba.fastjson.JSON;
import com.github.hambuger.memory.chat.memory.chat.SpringAiChat;
import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.other.functionCall.aop.FunctionCallRegistry;
import com.github.hambuger.memory.chat.memory.other.prompt.PromptFactory;
import com.github.hambuger.memory.chat.memory.other.util.RedisUtil;
import com.github.hambuger.memory.chat.memory.other.util.UserInfoUtil;
import com.github.hambuger.memory.chat.memory.plan.DayPlanGenerate;
import com.github.hambuger.memory.chat.memory.portrait.model.SelfPortrait;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    public static final String CUSTOM_SELF_PORTRAIT = "%s:custom:portrait";

    public static String getCustomSelfPortraitKey() {
        return String.format(CUSTOM_SELF_PORTRAIT, UserInfoUtil.getUser());
    }

    @FunctionCallRegistry(functionDesc = "Update profile data", scene = {ChatSceneEnum.UPDATE_SELF_PORTRAIT})
    public Boolean updatePortraitInfo(SelfPortrait param) {
        redisUtil.setString(getCustomSelfPortraitKey(), JSON.toJSONString(param));
        return true;
    }

    @FunctionCallRegistry(functionDesc = "Added character portrait data", scene = {ChatSceneEnum.ROLE_CHANGE})
    public Boolean addRole(SelfPortrait selfPortrait) {
        updateSelfPortrait(selfPortrait);
        redisUtil.reset(DayPlanGenerate.getCustomDayPlanKey());
        dayPlanGenerate.processPlanTasks();
        return true;
    }

    @FunctionCallRegistry(functionDesc = "Updated Andrew's self-portrait to be performed in parallel with the reply message", scene = {ChatSceneEnum.NORMAL_USER, ChatSceneEnum.NORMAL_GROUP})
    public Boolean updateSelfPortrait(SelfPortrait param) {
        String selfPortrait = redisUtil.getString(getCustomSelfPortraitKey());
        if (StringUtils.isNotBlank(selfPortrait)) {
            SelfPortrait selfPortraitObj = JSON.parseObject(selfPortrait, SelfPortrait.class);
            String beforePortrait = selfPortraitObj.toMarkDown();
            String afterPortrait = param.toMarkDown();
            String prompt = promptFactory.getSelfPortraitUpdatePrompt(beforePortrait, afterPortrait);
            List<OpenAiApi.ChatCompletionMessage> messages = new ArrayList<>();
            messages.add(new OpenAiApi.ChatCompletionMessage(prompt, OpenAiApi.ChatCompletionMessage.Role.SYSTEM));
            springAiChat.generateMsgWithMsgListAndFunctions(messages, false, ChatSceneEnum.UPDATE_SELF_PORTRAIT);
        }else {
            redisUtil.setString(getCustomSelfPortraitKey(), JSON.toJSONString(param));
        }
        return true;
    }


    public String getSelfPortrait() {
        String portraitStr = Optional.ofNullable(redisUtil.getString(String.format(CUSTOM_SELF_PORTRAIT, UserInfoUtil.getUser()))).orElse(redisUtil.getString(SELF_PORTRAIT_KEY));
        Map<Object, Object> hourPlanMap = redisUtil.getMap(DayPlanGenerate.getCustomDayPlanKey());
        int nowHour = DateUtil.thisHour(true);
        String doing = Optional.ofNullable(hourPlanMap).map(map -> {
            Object object = map.get(Integer.toString(nowHour));
            if (object == null) {
                return "Unknown";
            }
            StringBuilder info = new StringBuilder(object.toString());
            info.append("(");
            info.append("has passed ");
            int minute = DateUtil.minute(new Date());
            info.append(minute);
            info.append("minutes,");
            info.append(60 - minute);
            info.append("minutes left)");
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
