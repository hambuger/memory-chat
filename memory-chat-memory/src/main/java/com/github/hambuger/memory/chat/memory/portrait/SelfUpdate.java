package com.github.hambuger.memory.chat.memory.portrait;

import com.alibaba.fastjson.JSON;
import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.other.functionCall.aop.FunctionCallRegistry;
import com.github.hambuger.memory.chat.memory.other.util.RedisUtil;
import com.github.hambuger.memory.chat.memory.plan.DayPlanGenerate;
import com.github.hambuger.memory.chat.memory.portrait.model.SelfPortrait;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Date;
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


    @FunctionCallRegistry(functionDesc = "更新Andrew的自我画像，可与回复消息并行执行", scene = {ChatSceneEnum.NORMAL_USER, ChatSceneEnum.NORMAL_GROUP})
    public Boolean updateSelfPortrait(SelfPortrait param) {
        redisUtil.setString(SELF_PORTRAIT_KEY, JSON.toJSONString(param));
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
