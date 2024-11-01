package com.github.hambuger.memory.chat.memory.other.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;


@Slf4j
@Component
public class CFR2Utils {

    // 加载配置
    private static String accessKeyId = "fd57ce7068ff3cd70c3d571901eefbd0";
    private static String secretAccessKey = "f4e78cba4d29a7d30c212a56a11a2d60c1e4e85382fcafa9b1ee6aeb2f1374ef";
    private static String endpoint = "https://6ed53acaacf843c18d2fa8c5b242de2e.r2.cloudflarestorage.com";
    private static String bucketName = "data";

    // 创建S3客户端
   S3Client s3 = S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.of("auto"))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
            .build();


    public String uploadFile(File file) {


        // 上传文件
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(file.getName())
                .build();

        PutObjectResponse response = s3.putObject(
                putObjectRequest,
                software.amazon.awssdk.core.sync.RequestBody.fromFile(file)
        );

        // 拼接URL
        return "https://file.hamburgerhan.com/"+file.getName();

    }

    public String uploadFile(MultipartFile file, String fileName) {


        try {
            Path tempFile = Files.createTempFile("upload-", file.getOriginalFilename());

        file.transferTo(tempFile.toFile());

        // 上传文件
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        s3.putObject(
                putObjectRequest,
                software.amazon.awssdk.core.sync.RequestBody.fromFile(tempFile)
        );

        // 拼接URL
        return "https://file.hamburgerhan.com/"+fileName;
        } catch (IOException e) {
            log.error("uploadFile error", e);
        }
        return null;

    }

    public String uploadBase64(String base64, String fileName) {

        byte[] bI = Base64.getDecoder().decode((base64.substring(base64.indexOf(",") + 1)).getBytes());

        InputStream fis = new ByteArrayInputStream(bI);
        // 上传文件
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType("image/" + base64.split(";")[0].split("/")[1])
                .build();


        PutObjectResponse response = s3.putObject(
                putObjectRequest,
                software.amazon.awssdk.core.sync.RequestBody.fromInputStream(fis, Long.valueOf(bI.length)));
       

        // 拼接URL
        return "https://file.hamburgerhan.com/"+fileName;
    }

}
