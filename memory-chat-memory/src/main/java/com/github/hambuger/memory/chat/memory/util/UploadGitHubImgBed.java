package com.github.hambuger.memory.chat.memory.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class UploadGitHubImgBed {
    /**
     * Github私人令牌
     */
    @Value("${github.accessToken}")
    public String ACCESS_TOKEN;

    /**
     * github用户名
     */
    @Value("${github.owner}")
    public String OWNER;

    /**
     * 上传指定仓库
     */
    @Value("${github.repo}")
    public String REPO;


    /**
     * 上传时指定存放图片路径
     */
    @Value("${github.path}")
    public String PATH;


    /**
     * 用于提交描述
     */
    @Value("${github.addMessage}")
    public String ADD_MESSAGE;

    @Value("${github.delMessage}")
    public String DEL_MESSAGE;

    //API
    /**
     * 新建(POST)、获取(GET)、删除(DELETE)文件：()中指的是使用对应的请求方式
     * %s =>仓库所属空间地址(企业、组织或个人的地址path)  (owner)
     * %s => 仓库路径(repo)
     * %s => 文件的路径(path)
     */
    @Value("${github.apiCreatePost}")
    public String API_CREATE_POST;

    @Value("${github.branch}")
    public String BRANCH;

    @Value("${github.email}")
    public String EMAIL;

    @Value("${github.accept}")
    public String ACCEPT;

    @Value("${github.contentType}")
    public String CONTENTTYPE;


    public static String getFileExtension(String fileName) {
        int lastIndexOfDot = fileName.lastIndexOf('.');
        if (lastIndexOfDot == -1) {
            return "";
        }
        return fileName.substring(lastIndexOfDot);
    }

    /**
     * 生成创建(获取、删除)的指定文件路径
     *
     * @param originalFilename
     * @return
     */
    public String createUploadFileUrl(String originalFilename) {
        //获取文件后缀
        String suffix = getFileExtension(originalFilename);
        //拼接存储的图片名称
        String fileName = System.currentTimeMillis() + StringUtils.replace(UUID.randomUUID().toString(), "-", "") + suffix;
        //填充请求路径
        String url = String.format(API_CREATE_POST, OWNER, REPO, PATH + "/" + fileName);
        return url;
    }

    /**
     * 获取创建文件的请求体map集合：access_token、message、content
     */
    public Map<String, Object> getUploadBodyMap(String base64Data) {
        HashMap<String, Object> bodyMap = new HashMap<>(2);
        bodyMap.put("message", ADD_MESSAGE);
        bodyMap.put("content", base64Data);
        bodyMap.put("branch", BRANCH);
        bodyMap.put("committer", new HashMap<>() {{
            put("name", OWNER);
            put("email", EMAIL);
        }});
        return bodyMap;
    }

    public Map<String, String> getUploadHeader() {
        Map<String, String> header = new HashMap<>();
        // Github生成的token 参考GitHub API https://docs.github.com/cn/rest/reference/repos#create-or-update-file-contents
        header.put("Authorization", ACCESS_TOKEN);
        header.put("Accept", ACCEPT);
        header.put("Content-Type", CONTENTTYPE);
        return header;
    }
}


