package com.github.hambuger.memory.chat.memory.learn;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.hambuger.memory.chat.memory.chat.SpringAiChat;
import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.other.functionCall.CallFunctionRegistryFactory;
import com.github.hambuger.memory.chat.memory.other.functionCall.FunctionTool;
import com.github.hambuger.memory.chat.memory.other.functionCall.aop.FunctionCallRegistry;
import com.github.hambuger.memory.chat.memory.other.prompt.PromptFactory;
import com.github.hambuger.memory.chat.memory.other.util.RedisUtil;

import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import static com.github.hambuger.memory.chat.memory.other.functionCall.CallFunctionRegistryFactory.FUNCTION_CALL_MAP;
import static com.github.hambuger.memory.chat.memory.other.functionCall.CallFunctionRegistryFactory.FUNCTION_CALL_METHOD_MAP;


/**
 * @author hanjiabao
 * @since 2024/8/15
 */
@Slf4j
@Component
public class LearnSkill {

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private PromptFactory promptFactory;

    @Resource
    private SpringAiChat springAiChat;

    public void learnSkillProcess(String memory) {
        String prompt = promptFactory.getLearnSkillPrompt(memory);
        List<OpenAiApi.ChatCompletionMessage> messages = Lists.newArrayList(new OpenAiApi.ChatCompletionMessage(prompt, OpenAiApi.ChatCompletionMessage.Role.SYSTEM));
        springAiChat.generateMsgWithMsgListAndFunctions(messages, false, ChatSceneEnum.LEARN_SKILL);
    }


    @Data
    public static class LearnSkillQuery {

        @JsonPropertyDescription("技能名")
        @JsonProperty(required = true)
        private String skillName;

    }

    @Data
    public static class LearnSkillParam {

        @JsonPropertyDescription("是否需要沉淀新技能")
        @JsonProperty(required = true)
        private boolean needLearnSkill;

        @JsonPropertyDescription("技能的英文名")
        @JsonProperty(required = false)
        private String skillEnName;

        @JsonPropertyDescription("技能详情")
        @JsonProperty(required = false)
        private String skillDetail;

    }



    @PostConstruct
    public void init() {
        Set<String> allLearnSkill = redisUtil.getAllLearnSkill();
        if(CollectionUtils.isEmpty(allLearnSkill)){
            return;
        }
        Function<Object, Object> function = arg -> {
            try {
                return getLearnSkill((LearnSkillQuery) arg);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        String template = """
{
  "type": "object",
  "properties": {
    "skillName": {
      "type": "string",
      "enum": %s
    }
  },
  "required": ["skillName"]
}
""";
        String methodDesc = String.format(template, JSON.toJSONString(allLearnSkill));
        FunctionTool functionTool = new FunctionTool();
        functionTool.setScene(new ChatSceneEnum[]{ChatSceneEnum.NORMAL_GROUP, ChatSceneEnum.NORMAL_USER, ChatSceneEnum.LEARN_SKILL});
        functionTool.setFunctionTool(new OpenAiApi.FunctionTool(new OpenAiApi.FunctionTool.Function("获取学习过的技能说明", "getLearnSkill", methodDesc)));
        FUNCTION_CALL_METHOD_MAP.put("getLearnSkill", functionTool);
        FUNCTION_CALL_MAP.put("getLearnSkill", new CallFunctionRegistryFactory.MethodFunction(LearnSkillQuery.class, function));
    }

    @FunctionCallRegistry(functionDesc = "处理技能学习", scene = {ChatSceneEnum.LEARN_SKILL})
    public boolean learnSkillProcess(LearnSkillParam param) {
        if (param.needLearnSkill) {
            redisUtil.putLearnSkill(param.getSkillEnName(), param.getSkillDetail());
            init();
        }
        return true;
    }


    public String getLearnSkill(LearnSkillQuery query) {
        return redisUtil.getLearnSkill(query.getSkillName());
    }

}
