package com.github.hambuger.memory.chat.memory.other.util;


import net.coobird.thumbnailator.Thumbnails;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
public class PicBedUtil {

    @Value("${image.upload.telegraph_url:xxx}")
    private String uploadUrl;

    @Value("${temp.path}")
    private String tempPath;

    @Resource
    private ImageUploadUtils imageUploadUtils;

    @Value("${image.upload.use_github:true}")
    private boolean useGithub;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;


    private static final RestTemplate restTemplate = new RestTemplate();

    public String uploadImage(String file) {
        if (useGithub) {
            try {
                return imageUploadUtils.uploadImg(file);
            } catch (Exception e) {
                return null;
            }
        }
        String fileNameEnd = UUID.randomUUID() + file.substring(file.indexOf("."));
        String newImagePath = tempPath + File.separator + "image";
        File folder = new File(newImagePath);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        newImagePath = newImagePath + File.separator + fileNameEnd;
        try (FileInputStream fis = new FileInputStream(file); FileOutputStream fos = new FileOutputStream(newImagePath)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            fos.flush();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return "https://hamburgerhan.com/image/" + fileNameEnd;
    }


    public List<String> uploadImages(List<String> files) {
        List<String> result = new ArrayList<>();
        if (useGithub) {
            try {
                return imageUploadUtils.uploadImageList(files);
            } catch (Exception e) {
                return result;
            }
        }
        Map<String, String> imageUrlMap = new ConcurrentHashMap<>();
        files.parallelStream().forEach(file -> {
            imageUrlMap.put(file, uploadImage(file));
        });
        for (String image : files) {
            Optional.ofNullable(imageUrlMap.get(image)).ifPresent(result::add);
        }
        return result;
    }


    public File zipImage(String imagePath) {
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            return null;
        }
        long fileSize = imageFile.length();

        if (fileSize > MAX_FILE_SIZE) {
            try {
                return compressImage(imageFile);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
        return imageFile;
    }


    private File compressImage(File inputFile) throws IOException {
        String newFileName = tempPath + File.separator + "compressed_" + inputFile.getName();
        File outputFile = new File(newFileName);
        double quality = 1.0;
        while (outputFile.length() == 0 || outputFile.length() > MAX_FILE_SIZE) {
            quality -= 0.1;
            Thumbnails.of(inputFile).scale(1.0).outputQuality(quality).toFile(outputFile);
            if (quality <= 0.1) {
                break;
            }
        }
        return outputFile;
    }
}

