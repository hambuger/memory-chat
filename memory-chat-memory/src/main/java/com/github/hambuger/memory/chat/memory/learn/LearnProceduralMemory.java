package com.github.hambuger.memory.chat.memory.learn;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.other.functionCall.CallFunctionRegistryFactory;
import com.github.hambuger.memory.chat.memory.other.functionCall.FunctionTool;
import com.github.hambuger.memory.chat.memory.other.util.RedisUtil;

import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import cn.hutool.core.map.MapUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jep.MainInterpreter;
import jep.SharedInterpreter;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
public class LearnProceduralMemory {

    // pip install jep
    @Value("${python.jep.path}")
    private String JEP_PATH;

    @Resource
    private RedisUtil redisUtil;

    private static final String PROCEDURE_MEMORY_SKILL = "procedureMemorySkill";


    @PostConstruct
    public void init() {
        MainInterpreter.setJepLibraryPath(JEP_PATH);
        Map<Object, Object> skillMap = redisUtil.getMap(PROCEDURE_MEMORY_SKILL);
        if (MapUtil.isEmpty(skillMap)) {
            return;
        }
        for (Map.Entry<Object, Object> objectEntry : skillMap.entrySet()) {
            JSONObject jsonObject = JSON.parseObject(JSON.toJSONString(objectEntry.getValue()));
            OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(jsonObject.getString("description"), jsonObject.getString("name"), jsonObject.getObject("parameters",
                    Map.class));
            OpenAiApi.FunctionTool functionTool = new OpenAiApi.FunctionTool(function);
            FunctionTool customFunctionTool = new FunctionTool();
            customFunctionTool.setFunctionTool(functionTool);
            customFunctionTool.setScene(new ChatSceneEnum[]{ChatSceneEnum.NORMAL_GROUP, ChatSceneEnum.NORMAL_USER});
            Function<Map<String, Object>, String> methodFunction = x -> invokePythonFunction(functionTool.function().name(), x);
            CallFunctionRegistryFactory.registryFunction(customFunctionTool, Map.class, methodFunction);
        }
    }


    private static final Map<String, String> FUNCATION_NAME_AND_FILE_MAP = new HashMap<>() {
        {

        }
    };

    public String addNewCodeFunction(String functionName, Object arg){


        return null;
    }


    public  String invokePythonFunction(String functionName, Map<String, Object> args) {
        try (SharedInterpreter interp = new SharedInterpreter()) {
            interp.runScript(FUNCATION_NAME_AND_FILE_MAP.get(functionName));
            Object result = interp.invoke(functionName, args);
            return JSON.toJSONString(result);
        } catch (Exception e) {
            log.error("invokePythonFunction error", e);
            return "invokePythonFunction error:\n" + e.getMessage();
        }
    }

}

