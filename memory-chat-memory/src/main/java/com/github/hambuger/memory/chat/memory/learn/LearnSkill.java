package com.github.hambuger.memory.chat.memory.learn;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.other.functionCall.CallFunctionRegistryFactory;
import com.github.hambuger.memory.chat.memory.other.functionCall.FunctionTool;
import com.github.hambuger.memory.chat.memory.other.functionCall.aop.FunctionCallRegistry;
import com.github.hambuger.memory.chat.memory.other.util.RedisUtil;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

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


    @Data
    public static class LearnSkillQuery {

        @JsonPropertyDescription("技能名")
        @JsonProperty(required = true)
        private String skillName;

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
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "LearnSkillQuery",
  "type": "object",
  "properties": {
    "skillName": {
      "$ref": "#/definitions/SkillName",
      "description": "技能名"
    }
  },
  "required": ["skillName"],
  "definitions": {
    "SkillName": {
      "type": "string",
      "enum": %s
    }
  }
}
""";
        String methodDesc = String.format(template, JSON.toJSONString(allLearnSkill));
        FunctionTool functionTool = new FunctionTool();
        functionTool.setScene(new ChatSceneEnum[]{ChatSceneEnum.NORMAL_GROUP, ChatSceneEnum.NORMAL_USER});
        functionTool.setFunctionTool(new OpenAiApi.FunctionTool(new OpenAiApi.FunctionTool.Function("获取学习过的技能说明", "getLearnSkill", methodDesc)));
        FUNCTION_CALL_METHOD_MAP.put("getLearnSkill", functionTool);
        FUNCTION_CALL_MAP.put("getLearnSkill", new CallFunctionRegistryFactory.MethodFunction(LearnSkillQuery.class, function));
    }


    public String getLearnSkill(LearnSkillQuery query) {
        return redisUtil.getLearnSkill(query.getSkillName());
    }

}
