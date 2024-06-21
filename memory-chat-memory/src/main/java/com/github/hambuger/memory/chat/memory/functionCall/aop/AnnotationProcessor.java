package com.github.hambuger.memory.chat.memory.functionCall.aop;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.function.Function;

import dev.langchain4j.agent.tool.ToolParameters;
import dev.langchain4j.agent.tool.ToolSpecification;
import com.github.hambuger.memory.chat.memory.util.CallFunctionRegistryFactory;


/**
 * @author hamburger
 * @since 2024/6/17
 */
@Component
public class AnnotationProcessor implements BeanPostProcessor {

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
                CallFunctionRegistryFactory.registryFunction(convertToolSpecification(method.getName(), functionCallRegistry.functionDesc(), argClass), argClass, function);
            }
        }
        return bean;
    }


    private ToolSpecification convertToolSpecification(String methodName, String methodDesc, Class<?> argClass) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        JsonSchemaGenerator jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper);

        JsonNode jsonSchema = jsonSchemaGenerator.generateJsonSchema(argClass);
        ToolParameters toolParameters = null;
        try {
            toolParameters = objectMapper.treeToValue(jsonSchema, ToolParameters.Builder.class).build();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return ToolSpecification.builder().name(methodName).description(methodDesc).parameters(toolParameters).build();
    }
}
