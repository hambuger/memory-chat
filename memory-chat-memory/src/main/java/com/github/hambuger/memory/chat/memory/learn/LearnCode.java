package com.github.hambuger.memory.chat.memory.learn;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;


/**
 * @author hanjiabao
 * @since 2024/8/15
 */
public class LearnCode {

    public static void loadJar(File jarFile) throws MalformedURLException {
        URL jarURL = jarFile.toURI().toURL();
        URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();

        try {
            // 使用反射访问URLClassLoader的私有addURL方法
            Method declaredMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            declaredMethod.setAccessible(true);
            declaredMethod.invoke(classLoader, jarURL);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load JAR file", e);
        }
    }

    public static void main(String[] args) {
        try {
            // 定义 Maven 命令来下载特定的 JAR 文件
            String groupId = "org.apache.commons";
            String artifactId = "commons-lang3";
            String version = "3.16.0";
            String downloadDirectory = "/Users/hamburger/.m2/repository/";

            // 创建下载目录
            File directory = new File(downloadDirectory);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // 构建 Maven 命令
            String mavenCommand = String.format("mvn org.apache.maven.plugins:maven-dependency-plugin:3.1.2:get " + "-Dartifact=%s:%s:%s " + "-Ddest=%s/%s-%s.jar", groupId, artifactId, version,
                    downloadDirectory, artifactId, version);

            // 使用 ProcessBuilder 执行 Maven 命令
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("bash", "-c", mavenCommand);

            Process process = processBuilder.start();

            // 读取并输出 Maven 命令的执行结果
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            System.out.println("Maven command executed, exit code: " + exitCode);

            // 打印下载的 JAR 文件路径
            File downloadedJar = new File(downloadDirectory + groupId.replace(".", "/") + "/" + artifactId + "/" + version, artifactId + "-" + version + ".jar");
            if (downloadedJar.exists()) {
                System.out.println("Downloaded JAR file path: " + downloadedJar.getAbsolutePath());
            }else {
                System.out.println("JAR file not found in the specified directory.");
            }
            loadJar(downloadedJar);
            // 现在可以使用反射来加载类或执行方法
            URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            Class<?> clazz = Class.forName("com.example.YourClass", true, classLoader);
            Object instance = clazz.getDeclaredConstructor().newInstance();
            clazz.getDeclaredMethod("yourMethod").invoke(instance);



        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
