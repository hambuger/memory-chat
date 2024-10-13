package com.github.hambuger.memory.chat.memory.other.util;

/**
 * @author hanjiabao
 * @since 2024/9/29
 */

import java.io.File;
import java.io.IOException;
import java.net.URI;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;


public class CFR2Utils {

    // 加载配置
    private static String accessKeyId = "c4d87d468fc28abf889b6dbaa960731c";
    private static String secretAccessKey = "29dfe56c5cd3609e3d3839912c8c8c5b88466ad079b5c3dd01560ca48648a1e2";
    private static String endpoint = "https://6ed53acaacf843c18d2fa8c5b242de2e.r2.cloudflarestorage.com";
    private static String bucketName = "file";


    public static void main(String[] args) throws IOException {
        uploadFile(new File("/Users/hamburger/Pictures/cat.jpg"));
    }

    public static String uploadFile(File file) throws IOException {
        // 创建S3客户端
        S3Client s3 = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.CN_NORTHWEST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .build();

        // 上传文件
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(file.getName())
                .build();

        PutObjectResponse response = s3.putObject(
                putObjectRequest,
                software.amazon.awssdk.core.sync.RequestBody.fromFile(file)
        );

        // 关闭S3客户端
        s3.close();
        // 拼接URL
        String url = "https://file.hamburgerhan.com/"+file.getName();
        return url;
    }
}
