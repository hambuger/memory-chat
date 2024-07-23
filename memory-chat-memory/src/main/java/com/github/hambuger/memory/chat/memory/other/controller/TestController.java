package com.github.hambuger.memory.chat.memory.other.controller;


import com.alibaba.fastjson.JSON;

import org.apache.commons.text.WordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;


/**
 * @author hamburger
 * @since 2024/6/15
 */
@Slf4j
@RestController
public class TestController {

    @Autowired
    private ApplicationContext applicationContext;


    @GetMapping("/hello")
    public String sayHello(@RequestParam(value = "name", defaultValue = "World") String name) {
        return "Hello, " + name + "!";
    }


    /**
     * 路径为serviceImpl类名称和方法名
     * 入参为方法入参
     */
    @PostMapping("/invoke/{serviceImplClassName}/{methodName}")
    public Object invokeServiceMethod(@PathVariable String serviceImplClassName, @PathVariable String methodName, @RequestBody(required = false) Object... params) {
        try {
            // 获取服务bean
            Object serviceBean = applicationContext.getBean(WordUtils.uncapitalize(serviceImplClassName));

            // 获取方法
            Method method =
                    Arrays.stream(serviceBean.getClass().getMethods()).filter(m -> m.getName().equals(methodName)).findFirst().orElseThrow(() -> new NoSuchMethodException("Method " + methodName +
                            " not found"));
            // 调用方法
            Object result;
            if (method.getParameterCount() == 0) {
                // 无参数方法
                result = method.invoke(serviceBean);
            }else {
                // 获取方法参数类型
                Class<?>[] parameterTypes = method.getParameterTypes();
                Object[] castedParams = new Object[params.length];

                // 参数类型转换
                for (int i = 0; i < params.length; i++) {
                    castedParams[i] = convertType(params[i], parameterTypes[i]);
                }
                result = method.invoke(serviceBean, castedParams);
            }
            return result;

        } catch (Exception e) {
            log.error("invoke test error", e);
            return e.getMessage();
        }
    }


    // 类型转换方法
    private Object convertType(Object param, Class<?> targetType) {
        if (targetType.isInstance(param)) {
            return param;
        }
        if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(param.toString());
        }
        if (targetType == Long.class || targetType == long.class) {
            return Long.parseLong(param.toString());
        }
        if (targetType == Double.class || targetType == double.class) {
            return Double.parseDouble(param.toString());
        }
        if (targetType == Float.class || targetType == float.class) {
            return Float.parseFloat(param.toString());
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.parseBoolean(param.toString());
        }
        if (targetType == String.class) {
            return String.valueOf(param);
        }
        // 可以根据需要添加更多类型转换
        return JSON.parseObject(JSON.toJSONString(param), targetType);
    }
}
