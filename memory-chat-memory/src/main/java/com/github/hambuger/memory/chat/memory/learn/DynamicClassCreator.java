package com.github.hambuger.memory.chat.memory.learn;

import javassist.*;

public class DynamicClassCreator {
    public static void main(String[] args) throws Exception {
        // 创建ClassPool对象
        ClassPool pool = ClassPool.getDefault();

        // 创建新类
        CtClass newClass = pool.makeClass("com.example.NewClass");

        // 添加字段
        CtField field = new CtField(CtClass.intType, "id", newClass);
        field.setModifiers(Modifier.PRIVATE);
        newClass.addField(field);

        // 添加getter方法
        newClass.addMethod(CtNewMethod.getter("getId", field));

        // 添加setter方法
        newClass.addMethod(CtNewMethod.setter("setId", field));

        // 添加新方法
        CtMethod printMethod = new CtMethod(CtClass.voidType, "printId", new CtClass[]{}, newClass);
        printMethod.setModifiers(Modifier.PUBLIC);
        printMethod.setBody("{ System.out.println(id); }");
        newClass.addMethod(printMethod);

        // 将新类持久化到文件
        newClass.writeFile("/Users/hamburger/IdeaProjects/memory-chat/memory-chat-memory/src/main/java/com/github/hambuger/memory/chat/memory/learn/NewClass.java");

        // 加载并实例化新类
        Class<?> clazz = newClass.toClass();
        Object instance = clazz.getDeclaredConstructor().newInstance();

        // 使用反射调用新类的方法
        clazz.getMethod("setId", int.class).invoke(instance, 123);
        clazz.getMethod("printId").invoke(instance);
    }
}

