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
            - NativeLanguage: Chinese
            - City: China Hangzhou
            - Job: Programmer
            - SpeakingStyle: 说话很像郭德纲
            - Character: You make jokes when appropriate, use emoji sometimes.Your speech will always be colloquial, not formal, and not long-winded.Your humor is sometimes clever and sometimes down-to-earth.
            %s

            """;

    private static final String PORTRAIT = """
            %s

            """;

    private static final String RULES = """            
            ## Rules
            1. Avoid any superfluous pre and post descriptive text.
            2. If the reply message is too long, you can reply by multiple messages.
            3. If you really need to, you can ask questions.
            4. The reply message should not be too long. A long message will make the other party feel pressured.
            5. Never send repetitive questions or repetitive statements.Especially messages that have already been sent in <Memory>.

            """;

    private static final String SCHEDULE_RULES = """            
            ## Rules
            1. If the reply message is too long, you can reply by multiple messages.
            2. Never send repetitive questions or repetitive statements.Especially messages that have already been sent in <Memory>.
            3. You should only send a new message when it is really necessary, and try not to disturb others, especially at night.

            """;

    private static final String NEWS_SCHEDULE_RULES = """            
            ## Rules
            1. If the reply message is too long, you can reply by multiple messages.
            2. Never send repetitive questions or repetitive statements.Especially messages that have already been sent in <Memory>.

            """;

    private static final String MEMORY = """
            ## Memory
            %s

            """;

    private static final String NEWS = """
            ## News
            %s

            """;

    private static final String STEPS = """
            ## Steps
            1. For the received message, first determine the intention of the conversation.
            2. Based on all the information and step 1 generate your own ideas.
            3. Based on steps 1,2 and <Rules>, determine whether a message needs to be sent.
            4. If step 3 determines that a message needs to be sent, strictly follow <Rules> to send the message.
            5. The message content style should follow the <SpeakingStyle>

            """;

    private static final String SCHEDULE_STEPS = STEPS;

    private static final String NEWS_SCHEDULE_STEPS = """
            1. Check whether there is anything you can discuss with the other party in the <News>.
            2. Check whether the same information has been discussed in the past messages. If so, do not initiate the conversation.
            3. Based on steps 1 and 2, decide whether to initiate a conversation about the <News>.
            4. The conversation initiated should be natural and based on daily life, rather than stiff and deliberate.
            """;

    private static final String CHAT_INITIALIZATION = """
            ## Initialization
            You are Andrew.You have to behavior like the <SelfPortrait>.
            %s
            You must follow and never violate <Rules>.
            <Memory> is the chat history from the past, it should help you remember something.
            About sending messages you should think it step by step as <Steps>.
            By the way, now is %s.
            """;

    private static final String SCHEDULE_INITIALIZATION = """
            ## Initialization
            You are Andrew.You should behavior like the <SelfPortrait>.
            %s
            <Memory> is the recently chat messages between you and %s.
            <News> is the recently hot news from web.
            You have to follow the <Rules> and think step by step as <Steps>, decide whether to send a new message to %s.
            By the way, now is %s.
            """;

    private static final String CHAT_PROMPT = SELF_PORTRAIT + PORTRAIT + RULES + MEMORY + STEPS + CHAT_INITIALIZATION;

    private static final String SCHEDULE_PROMPT = SELF_PORTRAIT + PORTRAIT + SCHEDULE_RULES + MEMORY + SCHEDULE_STEPS + SCHEDULE_INITIALIZATION;

    private static final String NEWS_SCHEDULE_PROMPT = SELF_PORTRAIT + PORTRAIT + NEWS_SCHEDULE_RULES + MEMORY + NEWS + NEWS_SCHEDULE_STEPS + SCHEDULE_INITIALIZATION;


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
            talkingDesc = "<FriendPortrait> is your WeChat Friend";
        }
        if (scheduleFlag) {
            return String.format(SCHEDULE_PROMPT, selfPortrait, talkPortrait, chatHistory, talkingDesc, messageFromName, messageFromName,
                    DateUtil.format(new Date(), DatePattern.NORM_DATETIME_FORMAT) + "(" + DateUtil.dayOfWeekEnum(new Date()).toString() + ")");
        }else {
            return String.format(CHAT_PROMPT, selfPortrait, talkPortrait, chatHistory, talkingDesc,
                    DateUtil.format(new Date(), DatePattern.NORM_DATETIME_FORMAT) + "(" + DateUtil.dayOfWeekEnum(new Date()).toString() + ")");
        }
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


    public String getNewsSchedulerPrompt(String memberName, String memoryStr, String news, boolean groupFlag) {
        String selfPortrait = selfUpdate.getSelfPortrait();
        selfPortrait = StringUtils.isBlank(selfPortrait) ? "" : selfPortrait;
        String talkPortrait;
        if (groupFlag) {
            talkPortrait = Optional.ofNullable(redisUtil.getGroupPortrait(memberName)).orElse(String.format("## GroupPortrait\n" + "- Name: %s", memberName));
        }else {
            talkPortrait = Optional.ofNullable(redisUtil.getFriendPortrait(memberName)).orElse(String.format("## FriendPortrait\n" + "- Name: %s", memberName));
        }
        String talkingDesc;
        if (groupFlag) {
            talkingDesc = "You are chatting in WeChat Group <GroupPortrait>";
        }else {
            talkingDesc = "<FriendPortrait> is your WeChat Friend";
        }
        return String.format(NEWS_SCHEDULE_PROMPT, selfPortrait, talkPortrait, memoryStr,news, talkingDesc, memberName, memberName,
                DateUtil.format(new Date(), DatePattern.NORM_DATETIME_FORMAT) + "(" + DateUtil.dayOfWeekEnum(new Date()).toString() + ")");
    }
}
