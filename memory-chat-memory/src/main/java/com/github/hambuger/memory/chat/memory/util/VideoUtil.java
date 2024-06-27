package com.github.hambuger.memory.chat.memory.util;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class VideoUtil {

    private static List<String> getVideoImg(String filePath, String fileName) {
        File folder = new File(filePath);
        if (!folder.exists()) {
            folder.mkdirs(); // 如果文件夹不存在则创建
        }
        try {
            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(filePath + File.separator + fileName);
            grabber.start();

            double frameRate = grabber.getFrameRate();
            double videoLengthInSeconds = grabber.getLengthInTime() / 1000000.0;
            int totalFrames = (int) (videoLengthInSeconds * frameRate);

            double interval = videoLengthInSeconds / 100; // 计算间隔，保证最多100帧
            if (interval < 0.5) {
                interval = 0.5; // 保证最小间隔0.5秒
            }
            int frameInterval = (int) (interval * frameRate); // 根据间隔计算帧数间隔

            int frameNumber = 0;
            int count = 0;
            List<String> imageFilePathList = new ArrayList<>();
            while (frameNumber < totalFrames && count < 100) {
                grabber.setFrameNumber(frameNumber);
                Frame frame = grabber.grabImage();
                if (frame == null) {
                    break;
                }

                String rotate = grabber.getVideoMetadata("rotate");
                Java2DFrameConverter converter = new Java2DFrameConverter();
                BufferedImage bufferedImage = converter.getBufferedImage(frame);
                if (rotate != null) {
                    bufferedImage = rotate(bufferedImage, Integer.parseInt(rotate));
                }
                String newFileName = filePath + File.separator + fileName.substring(0, fileName.lastIndexOf(".")) + "_" + count + ".jpeg";
                ImageIO.write(bufferedImage, "jpeg", new File(newFileName));
                frameNumber += frameInterval;
                count++;
                imageFilePathList.add(newFileName);
            }

            grabber.close();
            grabber.stop();
        } catch (Exception e) {
            log.info("获取视频图片失败", e);
        }
        return null;
    }


    private static BufferedImage rotate(BufferedImage src, int angle) {
        int srcWidth = src.getWidth(null);
        int srcHeight = src.getHeight(null);
        int type = src.getColorModel().getTransparency();
        Rectangle rectDes = calcRotatedSize(new Rectangle(new Dimension(srcWidth, srcHeight)), angle);
        BufferedImage bi = new BufferedImage(rectDes.width, rectDes.height, type);
        Graphics2D g2 = bi.createGraphics();
        g2.translate((rectDes.width - srcWidth) / 2, (rectDes.height - srcHeight) / 2);
        g2.rotate(Math.toRadians(angle), srcWidth / 2, srcHeight / 2);
        g2.drawImage(src, 0, 0, null);
        g2.dispose();
        return bi;
    }


    private static Rectangle calcRotatedSize(Rectangle src, int angle) {
        if (angle >= 90) {
            if (angle / 90 % 2 == 1) {
                int temp = src.height;
                src.height = src.width;
                src.width = temp;
            }
            angle = angle % 90;
        }
        double r = Math.sqrt(src.height * src.height + src.width * src.width) / 2;
        double len = 2 * Math.sin(Math.toRadians(angle) / 2) * r;
        double angleAlpha = (Math.PI - Math.toRadians(angle)) / 2;
        double angleDeltaWidth = Math.atan((double) src.height / src.width);
        double angleDeltaHeight = Math.atan((double) src.width / src.height);
        int lenDeltaWidth = (int) (len * Math.cos(Math.PI - angleAlpha - angleDeltaWidth));
        int lenDeltaHeight = (int) (len * Math.cos(Math.PI - angleAlpha - angleDeltaHeight));
        int desWidth = src.width + lenDeltaWidth * 2;
        int desHeight = src.height + lenDeltaHeight * 2;
        return new Rectangle(new Dimension(desWidth, desHeight));
    }


    public static String extractVideoAudio(String videoFilePath) {
        File file = new File(videoFilePath);
        // 抓取资源
        FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(videoFilePath);
        Frame frame;
        FFmpegFrameRecorder recorder;
        String fileName;
        File outputFile;
        FileOutputStream fos = null;

        try {
            frameGrabber.start();
            // 输出位置
            fileName = file.getAbsolutePath() + "_audio.mp3";
            outputFile = new File(fileName);
            fos = new FileOutputStream(outputFile);
            recorder = new FFmpegFrameRecorder(fos, frameGrabber.getAudioChannels());
            recorder.setFormat("mp3");
            recorder.setSampleRate(frameGrabber.getSampleRate());
            recorder.setAudioQuality(0);

            recorder.start();
            while (true) {
                frame = frameGrabber.grab();
                if (frame == null) {
                    break;
                }
                if (frame.samples != null) {
                    recorder.recordSamples(frame.sampleRate, frame.audioChannels, frame.samples);
                }
                if (outputFile.length() > 25 * 1024 * 1024) { // 检查文件大小是否超过25M
                    break;
                }
            }
            recorder.stop();
            recorder.release();
            frameGrabber.stop();
            fos.close();
            return fileName;
        } catch (Exception e) {
            log.error("extractVideoAudio error", e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    log.error("extractVideoAudio error", e);
                }
            }
        }
        return null;
    }

}

