package com.github.hambuger.memory.chat.memory.other.functionCall.aop;

import com.github.hambuger.memory.chat.memory.chat.model.ChatSceneEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * @author hamburger
 * @since 2024/6/17
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FunctionCallRegistry {

    String functionDesc() default "";

    ChatSceneEnum[] scene();

}
