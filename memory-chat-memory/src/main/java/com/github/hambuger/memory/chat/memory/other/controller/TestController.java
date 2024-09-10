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
     * The path is the serviceImpl class name and method name
     * The input parameters are method input parameters
     */
    @PostMapping("/invoke/{serviceImplClassName}/{methodName}")
    public Object invokeServiceMethod(@PathVariable String serviceImplClassName, @PathVariable String methodName, @RequestBody(required = false) Object... params) {
        try {

            Object serviceBean = applicationContext.getBean(WordUtils.uncapitalize(serviceImplClassName));


            Method method =
                    Arrays.stream(serviceBean.getClass().getMethods()).filter(m -> m.getName().equals(methodName)).findFirst().orElseThrow(() -> new NoSuchMethodException("Method " + methodName +
                            " not found"));

            Object result;
            if (method.getParameterCount() == 0) {

                result = method.invoke(serviceBean);
            }else {

                Class<?>[] parameterTypes = method.getParameterTypes();
                Object[] castedParams = new Object[params.length];


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

        return JSON.parseObject(JSON.toJSONString(param), targetType);
    }
}
