package com.github.hambuger.memory.chat.memory.learn;


import com.google.common.collect.Lists;

import cn.hutool.core.map.MapUtil;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.hambuger.memory.chat.memory.chat.SpringAiChat;
import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.other.functionCall.CallFunctionRegistryFactory;
import com.github.hambuger.memory.chat.memory.other.functionCall.FunctionTool;
import com.github.hambuger.memory.chat.memory.other.functionCall.aop.FunctionCallRegistry;
import com.github.hambuger.memory.chat.memory.other.prompt.PromptFactory;
import com.github.hambuger.memory.chat.memory.other.util.RedisUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jep.JepConfig;
import jep.MainInterpreter;
import jep.SharedInterpreter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;


@Slf4j
@Component
public class LearnProceduralMemory {

    // pip install jep
    @Value("${python.jep.path}")
    private String JEP_PATH;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private SpringAiChat springAiChat;

    @Resource
    private PromptFactory promptFactory;

    private static final String PROCEDURE_MEMORY_SKILL = "procedureMemorySkill";

    private static final String PYTHON_PATH = Paths.get("memory-chat-memory/src/main/java/com/github/hambuger/memory/chat/memory/tools/pythons").toAbsolutePath().toString();

    @Data
    public static class FunctionDefinitionAndCode {

        @JsonPropertyDescription("方法名，英文命名")
        @JsonProperty(required = true)
        private String name;

        @JsonPropertyDescription("方法功能描述")
        @JsonProperty(required = true)
        private String description;

        @JsonPropertyDescription("方法参数的jsonSchema定义")
        @JsonProperty(required = true)
        private Map<String, Object> parameters;

        @JsonPropertyDescription("方法代码执行需要安装的包")
        @JsonProperty(required = true)
        private List<String> packages;

        @JsonPropertyDescription("方法的全部代码,不包含测试代码")
        @JsonProperty(required = true)
        private String functionCode;

        @JsonIgnore
        private String filepath;


    }

    static {
        SharedInterpreter.setConfig(new JepConfig().addIncludePaths(PYTHON_PATH));
    }

    @PostConstruct
    public void init() {
        MainInterpreter.setJepLibraryPath(JEP_PATH);
        Map<Object, Object> skillMap = redisUtil.getMap(PROCEDURE_MEMORY_SKILL);
        if (MapUtil.isEmpty(skillMap)) {
            return;
        }
        for (Map.Entry<Object, Object> objectEntry : skillMap.entrySet()) {
            FunctionDefinitionAndCode jsonObject = (FunctionDefinitionAndCode) objectEntry.getValue();
            OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(jsonObject.getDescription(), jsonObject.getName(), jsonObject.getParameters());
            OpenAiApi.FunctionTool functionTool = new OpenAiApi.FunctionTool(function);
            FunctionTool customFunctionTool = new FunctionTool();
            customFunctionTool.setFunctionTool(functionTool);
            customFunctionTool.setScene(new ChatSceneEnum[]{ChatSceneEnum.NORMAL_GROUP, ChatSceneEnum.NORMAL_USER});
            Function<Map<String, Object>, String> methodFunction = x -> invokePythonFunction(functionTool.function().name(), x);
            CallFunctionRegistryFactory.registryFunction(customFunctionTool, Map.class, methodFunction);
        }
    }


    public void learnCodeSkillProcess(String memory) {
        String prompt = promptFactory.getLearnCodeSkillPrompt(memory);
        List<OpenAiApi.ChatCompletionMessage> messages = Lists.newArrayList(new OpenAiApi.ChatCompletionMessage(prompt, OpenAiApi.ChatCompletionMessage.Role.SYSTEM));
        springAiChat.generateMsgWithMsgListAndFunctions(messages, false, ChatSceneEnum.LEARN_JUDGE);
    }

    @FunctionCallRegistry(functionDesc = "新增一个程序方法", scene = {ChatSceneEnum.LEARN_FUNCTION})
    public Boolean addNewFunction(FunctionDefinitionAndCode param) {
        String filePath = writePythonCodeToFile(param.getName(), param.getFunctionCode());
        param.setFilepath(filePath);
         redisUtil.putKeyValue(PROCEDURE_MEMORY_SKILL, param.getName(), param);
         init();
        return true;
    }


    public static String writePythonCodeToFile(String methodName, String pythonCode) {
        // 生成文件名：方法名.py
        String fileName = methodName + ".py";
        // 拼接文件路径
        String filePath = Paths.get(PYTHON_PATH, fileName).toString();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(pythonCode);
        } catch (IOException e) {
            log.error("writePythonCodeToFile error", e);
        }
        return filePath;
    }

    @Data
    public static class FunctionCodeTest {

        @JsonPropertyDescription("方法名，英文命名")
        @JsonProperty(required = true)
        private String methodName;

        @JsonPropertyDescription("方法的全部代码")
        @JsonProperty(required = true)
        private String functionCode;

        @JsonPropertyDescription("方法的测试输入参数")
        @JsonProperty(required = true)
        private Map<String, Object> functionArgs;


    }


    @FunctionCallRegistry(functionDesc = "学习并持久化一个技能，通过代码的方式", scene = {ChatSceneEnum.LEARN_JUDGE})
    public Boolean learnAndSaveAsSkill(LearnSKillParam param){
        String prompt = """
Generate python code for the skill: %s.
1. use 'installPackages' to install the modules that code required
2. use 'executePythonCode' to check whether the code is correct
3. use 'addNewFunction' to save the final generated code
4. If you need outside help，you can use Baidu to query the required information and repair errors
5. If the error happened, try to fix it.
6. If a same error happened many times,please think carefully and modify the code from another way
7. The final saved code needs to be all the code required for a complete .py file.(No test code included)
Do it and think step by step.
""";
        List<OpenAiApi.ChatCompletionMessage> messages = new ArrayList<>();
        messages.add(new OpenAiApi.ChatCompletionMessage(String.format(prompt, param.getSkillDescription()),OpenAiApi.ChatCompletionMessage.Role.SYSTEM));
        springAiChat.generateMsgWithMsgListAndFunctions(messages, false, ChatSceneEnum.LEARN_FUNCTION);
        return true;
    }

    // 执行Python代码块并返回结果
    @FunctionCallRegistry(functionDesc = "测试代码是否正确", scene = {ChatSceneEnum.LEARN_FUNCTION})
    public String executePythonCode(FunctionCodeTest test) {
        String filePath = writePythonCodeToFile(test.getMethodName(), test.getFunctionCode());
        try (SharedInterpreter interp = new SharedInterpreter()) {
            interp.runScript(filePath);
            Object result = interp.invoke(test.getMethodName(), test.getFunctionArgs());
            return JSON.toJSONString(result);
        } catch (Exception e) {
            log.error("invokePythonFunction error", e);
            return "invokePythonFunction error:\n" + e.getMessage();
        }
    }

    @Data
    public static class InstallPackagesParam {

        @JsonPropertyDescription("方法代码执行需要安装的包")
        @JsonProperty(required = true)
        private List<String> packageNames;

    }


    @FunctionCallRegistry(functionDesc = "执行代码前，安装需要的包", scene = {ChatSceneEnum.LEARN_FUNCTION})
    public String installPackages(InstallPackagesParam param) {
        try (SharedInterpreter interp = new SharedInterpreter()) {
            interp.exec("import subprocess");

            for (String packageName : param.getPackageNames()) {
                String command = "subprocess.check_call(['pip', 'install', '" + packageName + "'])";
                interp.exec(command);
            }
        } catch (Exception e) {
            log.error("installPackage error", e);
            return "installPackage error:\n" + e.getMessage();
        }
        return "installPackage success";
    }


    public String invokePythonFunction(String functionName, Map<String, Object> args) {
        try (SharedInterpreter interp = new SharedInterpreter()) {
            interp.runScript(((FunctionDefinitionAndCode) redisUtil.getMap(PROCEDURE_MEMORY_SKILL).get(functionName)).getFilepath());
            Object result = interp.invoke(functionName, args);
            return JSON.toJSONString(result);
        } catch (Exception e) {
            log.error("invokePythonFunction error", e);
            return "invokePythonFunction error:\n" + e.getMessage();
        }
    }

    public String invokePythonFunction(List<String> filePaths, String functionName, Map<String, Object> args) {
        try (SharedInterpreter interp = new SharedInterpreter()) {
            interp.set("scriptPath", PYTHON_PATH);
            for (String filePath : filePaths) {
                interp.runScript(filePath);
            }
            Object result = interp.invoke(functionName, args);
            return JSON.toJSONString(result);
        } catch (Exception e) {
            log.error("invokePythonFunction error", e);
            return "invokePythonFunction error:\n" + e.getMessage();
        }
    }

}

