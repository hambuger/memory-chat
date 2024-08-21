package com.github.hambuger.memory.chat.memory.learn;


import cn.hutool.core.map.MapUtil;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.other.functionCall.CallFunctionRegistryFactory;
import com.github.hambuger.memory.chat.memory.other.functionCall.FunctionTool;
import com.github.hambuger.memory.chat.memory.other.functionCall.aop.FunctionCallRegistry;
import com.github.hambuger.memory.chat.memory.other.util.RedisUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
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

    private static final String PROCEDURE_MEMORY_SKILL = "procedureMemorySkill";

    @Data
    public class FunctionDefinitionAndCode {

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

        @JsonPropertyDescription("方法的全部代码")
        @JsonProperty(required = true)
        private String functionCode;

        @JsonIgnore
        private String filepath;


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

    @FunctionCallRegistry(functionDesc = "新增一个程序方法", scene = {ChatSceneEnum.LEARN_FUNCTION})
    public Boolean addNewFunction(FunctionDefinitionAndCode param) {
        String filePath = writePythonCodeToFile(param.getName(), param.getFunctionCode());
        param.setFilepath(filePath);
        redisUtil.putKeyValue(PROCEDURE_MEMORY_SKILL, param.getName(), param);
        init();
        return true;
    }


    public String writePythonCodeToFile(String methodName, String pythonCode) {
        // 生成文件名：方法名.py
        String fileName = methodName + ".py";
        // 获取当前目录的绝对路径
        String currentDir = Paths.get("").toAbsolutePath().toString();
        // 拼接文件路径
        String filePath = Paths.get(currentDir, fileName).toString();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(pythonCode);
        } catch (IOException e) {
            log.error("writePythonCodeToFile error", e);
        }
        return filePath;
    }

    @Data
    public class FunctionCodeTest {

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
    public class InstallPackagesParam {

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

}

