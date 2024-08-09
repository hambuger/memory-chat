package com.github.hambuger.memory.chat.memory.other.functionCall.aop;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;
import com.github.hambuger.memory.chat.memory.other.functionCall.FunctionTool;
import com.github.hambuger.memory.chat.memory.other.functionCall.CallFunctionRegistryFactory;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.function.Function;


/**
 * @author hamburger
 * @since 2024/6/17
 */
@Component
public class AnnotationProcessor implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(AnnotationProcessor.class);


    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        Class<?> clazz = bean.getClass();
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(FunctionCallRegistry.class)) {
                FunctionCallRegistry functionCallRegistry = method.getAnnotation(FunctionCallRegistry.class);
                // 获取方法的第一个参数类型
                Class<?> argClass = method.getParameterTypes()[0];

                // 封装成函数式方法
                Function<Object, Object> function = arg -> {
                    try {
                        return method.invoke(bean, arg);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };
                ChatSceneEnum[] scene = functionCallRegistry.scene();
                CallFunctionRegistryFactory.registryFunction(convertToolSpecification(method.getName(), functionCallRegistry.functionDesc(), argClass, scene), argClass, function);

            }
        }
        return bean;
    }


    private FunctionTool convertToolSpecification(String methodName, String methodDesc, Class<?> argClass, ChatSceneEnum[] scene) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        JsonSchemaGenerator jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper);
        JsonNode jsonSchema = jsonSchemaGenerator.generateJsonSchema(argClass);
        FunctionTool functionTool = new FunctionTool();
        functionTool.setScene(scene);
        try {
            functionTool.setFunctionTool(new OpenAiApi.FunctionTool(new OpenAiApi.FunctionTool.Function(methodDesc, methodName, objectMapper.writeValueAsString(jsonSchema))));
        } catch (JsonProcessingException e) {
            log.error("convertToolSpecification error", e);
            return null;
        } return functionTool;
    }
}
