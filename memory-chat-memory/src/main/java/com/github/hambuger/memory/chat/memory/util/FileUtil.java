package com.github.hambuger.memory.chat.memory.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * @author hanjiabao
 * @since 2024/6/21
 */
public class FileUtil {

    public static String downloadImage(String imageUrl) {
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        String imagePath = null;

        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            // 检查连接是否成功
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                System.out.println("Failed to connect to the URL");
                return null;
            }

            // 获取输入流
            inputStream = connection.getInputStream();

            // 定义文件路径
            imagePath = "downloaded_image.jpg";
            File imageFile = new File(imagePath);
            outputStream = new FileOutputStream(imageFile);

            // 写入文件
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            System.out.println("Image downloaded to: " + imagePath);
        } catch (Exception e) {
            e.printStackTrace();
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
                e.printStackTrace();
            }
        }

        return imagePath;
    }


//    public static void fileDownloadWithConsumer(String url, Consumer<String> consumer) {
//
//        String filePath = downloadImage(url);
//        consumer.accept(filePath);
//        deleteImage(filePath);
//
//    }


    public static void deleteImage(String imagePath) {
        File imageFile = new File(imagePath);
        if (imageFile.delete()) {
            System.out.println("Image deleted successfully");
        }else {
            System.out.println("Failed to delete the image");
        }
    }

}
