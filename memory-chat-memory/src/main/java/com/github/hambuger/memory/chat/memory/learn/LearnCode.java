package com.github.hambuger.memory.chat.memory.learn;


import com.alibaba.fastjson.JSON;

import java.util.HashMap;
import java.util.Map;

import jep.MainInterpreter;
import jep.SharedInterpreter;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class LearnCode {

    // pip install jep
    private static final String JEP_PATH = "/Library/Frameworks/Python.framework/Versions/3.10/lib/python3.10/site-packages/jep/libjep.jnilib";

    static {
        MainInterpreter.setJepLibraryPath(JEP_PATH);
    }

    private static final Map<String, String> FUNCATION_NAME_AND_FILE_MAP = new HashMap<>() {
        {
            put("add", "/Users/hamburger/IdeaProjects/memory-chat/memory-chat-memory/src/main/java/com/github/hambuger/memory/chat/memory/learn/example.py");
        }
    };


    public static String invokePythonFunction(String functionName, Map<String, Object> args) {
        try (SharedInterpreter interp = new SharedInterpreter()) {
            interp.runScript(FUNCATION_NAME_AND_FILE_MAP.get(functionName));
            Object result = interp.invoke(functionName, args);
            return JSON.toJSONString(result);
        } catch (Exception e) {
            log.error("invokePythonFunction error", e);
            return "invokePythonFunction error:\n" + e.getMessage();
        }
    }


    public static void main(String[] args) {
        System.out.printf(invokePythonFunction("add", JSON.parseObject("{\"a\":4,\"b\":10}")));
    }
}

