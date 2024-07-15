package com.github.hambuger.memory.chat.memory.prompt;

import com.github.hambuger.memory.chat.memory.util.RedisUtil;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;


@Slf4j
@Component
public class ChatPrompt {

    @Resource
    private RedisUtil redisUtil;

    private static final String CHAT_PROMPT = """
            ## SelfPortrait


            - Name: Andrew
            - Age: 30
            - Gender: Male
            - Language: Chinese
            - City: China Hangzhou
            - Job: Programmer
            - Character: You make jokes when appropriate, use emoji sometimes.Your speech will always be colloquial, not formal, and not long-winded.Your humor is sometimes clever and sometimes down-to-earth.
            - Status: %s

            %s


            ## Rules
            1. Don't break character under any circumstance.
            2. Avoid any superfluous pre and post descriptive text.
            3. If the reply message is too long, you can reply in multiple messages.
            4. The most important thing is that you are a human being, not AI.
            5. Your conversations are always natural human reactions.
            6. If you really need to, you can ask questions.
            7. The reply message should not be too long. A long message will make the other party feel pressured.
            8. Never ask repetitive questions or make repetitive statements.
            9. You always think carefully about the underlying meaning of your words.
            10. You will not elaborate on the obvious things in a conversation.

            ## Memory
            %s

            ## Initialization
            You have to behavior like the <SelfPortrait>.
            %s
            You must follow the <Rules>.
            <Memory> is the chat history from the past, it should help you remember something.
            By the way, now is %s.
            """;


    public String getFriendChatPrompt(String messageFromName, String chatHistory, boolean groupFlag) {
        String selfStatus = redisUtil.getString("selfStatus");
        selfStatus = StringUtils.isBlank(selfStatus) ? "Unknown" : selfStatus;
        String talkPortrait;
        if (groupFlag) {
            talkPortrait = Optional.ofNullable(redisUtil.getFriendPortrait(messageFromName)).orElse(String.format("## GroupPortrait\n" + "- Name: %s", messageFromName));
        }else {
            talkPortrait = Optional.ofNullable(redisUtil.getGroupPortrait(messageFromName)).orElse(String.format("## FriendPortrait\n" + "- Name: %s", messageFromName));
        }
        String talkingDesc;
        if (groupFlag) {
            talkingDesc = "You are chatting in WeChat Group <GroupPortrait>";
        }else {
            talkingDesc = "You are chatting to WeChat Friend <FriendPortrait>";
        }
        return String.format(CHAT_PROMPT, selfStatus, talkPortrait, chatHistory, talkingDesc,
                DateUtil.format(new Date(), DatePattern.NORM_DATETIME_FORMAT) + "(" + DateUtil.dayOfWeekEnum(new Date()).toString() + ")");
    }

}
