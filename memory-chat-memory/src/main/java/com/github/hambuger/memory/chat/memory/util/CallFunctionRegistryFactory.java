package com.github.hambuger.memory.chat.memory.util;

import com.alibaba.fastjson.JSON;
import com.github.hambuger.memory.chat.memory.constants.CommonConstants;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import dev.langchain4j.agent.tool.ToolSpecification;
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


    public static volatile ConcurrentHashMap<String, ToolSpecification> FUNCTION_CALL_METHOD_MAP = new ConcurrentHashMap<>();

    public static volatile ConcurrentHashMap<String, MethodFunction> FUNCTION_CALL_MAP = new ConcurrentHashMap<>();


    public static boolean registryFunction(ToolSpecification toolSpecification, Class argClass, Function function) {
        FUNCTION_CALL_METHOD_MAP.put(toolSpecification.name(), toolSpecification);
        FUNCTION_CALL_MAP.put(toolSpecification.name(), new MethodFunction(argClass, function));
        return true;
    }


    public static List<ToolSpecification> getAllFunctionCall() {
        return FUNCTION_CALL_METHOD_MAP.values().stream().toList();
    }


    public static String executeFunctionResult(String functionName, String arg) {
        return Optional.ofNullable(FUNCTION_CALL_MAP.get(functionName)).map(function -> function.getFunction().apply(JSON.parseObject(arg, function.argClass)).toString()).orElse(CommonConstants.NULL_STR);
    }

}
