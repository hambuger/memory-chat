package com.github.hambuger.memory.chat.memory.functionCall.aop;

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

    String scope() default "";

}
