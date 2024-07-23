package com.github.hambuger.memory.chat.memory.functionCall;

import com.alibaba.fastjson.JSON;
import com.github.hambuger.memory.chat.memory.chat.dto.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.constants.CommonConstants;

import org.springframework.ai.openai.api.OpenAiApi;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Data;


/**
 * @author hamburger
 * @since 2024/6/17
 */
public class CallFunctionRegistryFactory {

    @Data
    @AllArgsConstructor
    public static class MethodFunction {

        private Class argClass;

        private Function function;

    }


    public static volatile ConcurrentHashMap<String, FunctionTool> FUNCTION_CALL_METHOD_MAP = new ConcurrentHashMap<>();

    public static volatile ConcurrentHashMap<String, MethodFunction> FUNCTION_CALL_MAP = new ConcurrentHashMap<>();


    public static boolean registryFunction(FunctionTool functionTool, Class argClass, Function function) {
        if (functionTool == null) {
            return false;
        }
        OpenAiApi.FunctionTool toolSpecification = functionTool.getFunctionTool();
        FUNCTION_CALL_METHOD_MAP.put(toolSpecification.function().name(), functionTool);
        FUNCTION_CALL_MAP.put(toolSpecification.function().name(), new MethodFunction(argClass, function));
        return true;
    }


    public static List<OpenAiApi.FunctionTool> getAllFunctionCall(boolean groupFlag, ChatSceneEnum scene) {
        return FUNCTION_CALL_METHOD_MAP.values().stream().filter(tool -> {
            boolean sceneFlag = true;
            if (!Arrays.stream(tool.getScene()).collect(Collectors.toSet()).contains(scene)) {
                sceneFlag = false;
            }
            if (groupFlag && !Arrays.stream(tool.getScene()).collect(Collectors.toSet()).contains(ChatSceneEnum.NORMAL_GROUP)) {
                sceneFlag = false;
            }
            return sceneFlag;
        }).map(FunctionTool::getFunctionTool).collect(Collectors.toList());
    }


    public static String executeFunctionResult(String functionName, String arg) {
        return Optional.ofNullable(FUNCTION_CALL_MAP.get(functionName)).map(function -> function.getFunction().apply(JSON.parseObject(arg, function.argClass)).toString()).orElse(CommonConstants.NULL_STR);
    }

}
