package io.github.memorychat.memory;

import com.alibaba.fastjson.JSONObject;

import org.apache.commons.lang3.StringUtils;

import io.github.memorychat.chat.LangChainChat;


/**
 * @author hamburger
 * @since 2024/6/13
 */
public class MemoryImportantScore {

    public static Double generateImportantScore(String message) {

        String prompt = "作为一款专属的AI聊天机器人，你的任务是建立与用户之间的深度、持久的联系。\n" + "\n" + "在````之间的内容是你要分析的信息内容，可能包括个人身份信息、情绪表达、问题询问或其他各种类型的信息。\n" + "\n" +
                "思考这些信息如何可能影响你未来与用户的对话。评估这些信息是否能够帮助你更深入地与用户建立紧密的交流，更准确地理解用户的需求、喜好以及情绪状态。\n" + "\n" + "在深入评估的基础上，根据你认为这些信息在未来对话检索中的重要性，为这些信息打分，分数范围为0-1。请注意，0表示这项信息对于长期的对话交流并无任何重要性，而1" +
                "则表示这项信息极其重要。请忽略这些信息在短期对话情景中的影响。返回一个打分的分数值score字段的json结构，不要提供其他信息。\n" + "\n" + "例如：\n" + "\n" + "用户:````晚安````\n" + "\n" + "AI:{\"score\":0.1}\n" + "\n" + "用户:````" + message +
                "````\n" + "\n" + "AI:";
        String score = JSONObject.parseObject(LangChainChat.generateJsonWithSingleMsgAndPrompt(prompt)).getString("score");
        if (StringUtils.isNotBlank(score)) {
            return Double.valueOf(score);
        }
        return 0.0;
    }

}
