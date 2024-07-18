package com.github.hambuger.memory.chat.memory.prompt;

import com.github.hambuger.memory.chat.memory.plan.SelfUpdate;
import com.github.hambuger.memory.chat.memory.util.RedisUtil;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
public class ChatPrompt {

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private SelfUpdate selfUpdate;

    private static final String SELF_PORTRAIT = """
            ## SelfPortrait


            - Name: Andrew
            - Age: 30
            - Gender: Male
            - Language: Chinese
            - City: China Hangzhou
            - Job: Programmer
            - Character: You make jokes when appropriate, use emoji sometimes.Your speech will always be colloquial, not formal, and not long-winded.Your humor is sometimes clever and sometimes down-to-earth.
            %s

            """;

    private static final String PORTRAIT = """
            %s


            """;

    private static final String RULES = """            
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

            """;

    private static final String SCHEDULE_RULES = """            
            ## Rules
             1. Don't break character under any circumstance.
             2. If the reply message is too long, you can reply in multiple messages.
             3. Never ask repetitive questions or make repetitive statements.
             4. You should only send a new message when it is really necessary, and try not to disturb others, especially at night.
             5. Don't send repeated messages.

            """;

    private static final String MEMORY = """
            ## Memory
            %s

            """;

    private static final String CHAT_INITIALIZATION = """
            ## Initialization
            You have to behavior like the <SelfPortrait>.
            %s
            You must follow the <Rules>.
            <Memory> is the chat history from the past, it should help you remember something.
            By the way, now is %s.
            """;

    private static final String SCHEDULE_INITIALIZATION = """
            ## Initialization
            You have to behavior like the <SelfPortrait>.
            %s
            <Memory> is the chat history from the past, it should help you remember something.
            You must follow the <Rules>,determine whether a new message needs to be sent.
            By the way, now is %s.
            """;

    private static final String CHAT_PROMPT = SELF_PORTRAIT + PORTRAIT + RULES + MEMORY + CHAT_INITIALIZATION;

    private static final String SCHEDULE_PROMPT = SELF_PORTRAIT + PORTRAIT + SCHEDULE_RULES + MEMORY + SCHEDULE_INITIALIZATION;


    public String getChatPrompt(String messageFromName, String chatHistory, boolean groupFlag, boolean scheduleFlag) {
        String selfPortrait = selfUpdate.getSelfPortrait();
        selfPortrait = StringUtils.isBlank(selfPortrait) ? "" : selfPortrait;
        String talkPortrait;
        if (groupFlag) {
            talkPortrait = Optional.ofNullable(redisUtil.getGroupPortrait(messageFromName)).orElse(String.format("## GroupPortrait\n" + "- Name: %s", messageFromName));
        }else {
            talkPortrait = Optional.ofNullable(redisUtil.getFriendPortrait(messageFromName)).orElse(String.format("## FriendPortrait\n" + "- Name: %s", messageFromName));
        }
        String talkingDesc;
        if (groupFlag) {
            talkingDesc = "You are chatting in WeChat Group <GroupPortrait>";
        }else {
            talkingDesc = "You are chatting to WeChat Friend <FriendPortrait>";
        }
        return String.format(scheduleFlag ? SCHEDULE_PROMPT : CHAT_PROMPT, selfPortrait, talkPortrait, chatHistory, talkingDesc,
                DateUtil.format(new Date(), DatePattern.NORM_DATETIME_FORMAT) + "(" + DateUtil.dayOfWeekEnum(new Date()).toString() + ")");
    }


    private static final String REFLECTION_PROMPT = """
            From following historical records, extract information similar to human long-term memory.
            %s
            Make sure your answer can be parsed correctly into json data similar to the following.
            %s
            The text represents the summarized and refined content. It should be more concise and shorter than the original text.
            p_ids represents all the information sources that the abstract relies on, obtained from parentheses at the beginning of each conversation.
            """;

    private static final String SCORE_PROMPT = """
            作为一款专属的AI聊天机器人，你的任务是建立与用户之间的深度、持久的联系。
            在````之间的内容是你要分析的信息内容，可能包括个人身份信息、情绪表达、问题询问或其他各种类型的信息。
            思考这些信息如何可能影响你未来与用户的对话。评估这些信息是否能够帮助你更深入地与用户建立紧密的交流，更准确地理解用户的需求、喜好以及情绪状态。
            在深入评估的基础上，根据你认为这些信息在未来对话检索中的重要性，为这些信息打分，分数范围为0-1。
            请注意，0表示这项信息对于长期的对话交流并无任何重要性，而1则表示这项信息极其重要。
            请忽略这些信息在短期对话情景中的影响。返回一个打分的分数值score字段的json结构，不要提供其他信息。

            例如：
            用户:````晚安````
            AI:{\\"score\\":0.1}

            用户:
            ````
            %s
            ````
            AI:
            """;


    public String getMsgReflectionPrompt(String... param) {
        return String.format(REFLECTION_PROMPT, param);
    }


    public String getMsgScorePrompt(String... param) {
        return String.format(SCORE_PROMPT, param);
    }

}
