package com.github.hambuger.memory.chat.memory.util;

import com.github.hambuger.memory.chat.wechat.api.DownloadTools;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

import static com.github.hambuger.memory.chat.memory.constants.CommonConstants.GET;
import static com.github.hambuger.memory.chat.memory.constants.Constants.SEND_IMAGE_PATH;


/**
 * @author hanjiabao
 * @since 2024/6/21
 */
@Slf4j
public class FileUtil {

    public static String downloadImage(String imageUrl) {
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        String imagePath;

        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(GET);
            connection.connect();

            // 检查连接是否成功
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }

            // 获取输入流
            inputStream = connection.getInputStream();

            // 定义文件路径
            File imageFile = new File(SEND_IMAGE_PATH);
            outputStream = new FileOutputStream(imageFile);

            // 写入文件
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            log.info("Image downloaded to:{}", SEND_IMAGE_PATH);
        } catch (Exception e) {
            log.error("downloadImage error", e);
            return null;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                log.error("downloadImage error", e);
            }
        }

        return SEND_IMAGE_PATH;
    }


    public static void deleteImage(String imagePath) {
        File imageFile = new File(imagePath);
        if (imageFile.delete()) {
            log.info("Image deleted successfully");
        } else {
            log.info("Failed to delete the image");
        }
    }

    public static String getFileBase64Data(String filePath, boolean await) {
        try {
            if (await) {
                DownloadTools.awaitDownload(filePath);
            }
            // 读取文件内容到字节数组
            byte[] fileContent = Files.readAllBytes(Paths.get(filePath));

            // 将字节数组编码为 Base64 字符串
            String base64String = Base64.getEncoder().encodeToString(fileContent);

            // 输出 Base64 字符串
            return base64String;
        } catch (IOException e) {
            log.error("downloadImage error", e);
        }
        return null;
    }
}
