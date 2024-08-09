package com.github.hambuger.memory.chat.memory.learn;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.tools.Diagnostic;


public class JavasistTest {

    public static void main(String[] args) throws Exception {
        //        //默认的类搜索路径
        //        ClassPool pool = ClassPool.getDefault();
        //
        //        //获取一个ctClass对象
        //        CtClass ctClass = pool.makeClass("Test");
        //        try {
        //            //添加age属性
        //            ctClass.addField(CtField.make("private int age;", ctClass));
        //            //添加setAge方法
        //            ctClass.addMethod(CtMethod.make("public void setAge(int age){this.age = age;}", ctClass));
        //            //添加getAge方法
        //            ctClass.addMethod(CtMethod.make("public int getAge(){return this.age;}", ctClass));
        //            //将ctClass生成字节数组，并写入文件
        //            byte[] byteArray = ctClass.toBytecode();
        //            FileOutputStream output = new FileOutputStream("/Users/hamburger/IdeaProjects/memory-chat/memory-chat-memory/src/main/java/com/github/hambuger/memory/chat/memory/learn/Test
        //            .class");
        //            output.write(byteArray);
        //            output.close();
        //            System.out.println("文件写入成功!!!");
        //        } catch (Exception e) {
        //            e.printStackTrace();
        //        }

        //https://github.com/square/javapoet
        try {
            MethodSpec methodSpec =
                    MethodSpec.methodBuilder("main").addModifiers(javax.lang.model.element.Modifier.PUBLIC, javax.lang.model.element.Modifier.STATIC).returns(void.class).addParameter(String[].class
                            , "args").addStatement("$T.out.println($S)", System.class, "hello world!").build();
            TypeSpec typeSpec = TypeSpec.classBuilder("HelloWorld").addModifiers(javax.lang.model.element.Modifier.PUBLIC, javax.lang.model.element.Modifier.FINAL).addMethod(methodSpec).build();

            JavaFile javaFile = JavaFile.builder("com.ldx.canvasdrawdemo", typeSpec).build();

//            Messager.printMessage(Diagnostic.Kind.NOTE, javaFile.toString() + "");

            javaFile.writeTo(new File("/Users/hamburger/IdeaProjects/memory-chat/memory-chat-memory/src/main/java/com/github/hambuger/memory/chat/memory/learn/Test.java"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

