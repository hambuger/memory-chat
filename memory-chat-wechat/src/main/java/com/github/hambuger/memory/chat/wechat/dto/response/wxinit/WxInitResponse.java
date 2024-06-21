/**
  * Copyright 2023 bejson.com 
  */
package com.github.hambuger.memory.chat.wechat.dto.response.wxinit;

import com.github.hambuger.memory.chat.wechat.dto.response.BaseResponse;
import com.github.hambuger.memory.chat.wechat.entity.Contacts;

import java.util.List;

import lombok.Data;


@Data
public class WxInitResponse {

    private BaseResponse BaseResponse;
    private int Count;
    private List<Contacts> ContactList;
    private com.github.hambuger.memory.chat.wechat.dto.response.sync.SyncKey SyncKey;
    private Contacts User;
    private String ChatSet;
    private String SKey;
    private long ClientVersion;
    private long SystemTime;
    private int GrayScale;
    private int InviteStartCount;
    private int MPSubscribeMsgCount;
    private List<MPSubscribeMsgList> MPSubscribeMsgList;
    private long ClickReportInterval;

    @Data
    public static class MPSubscribeMsgList {

        private String UserName;
        private int MPArticleCount;
        private List<MPArticleList> MPArticleList;
        private long Time;
        private String NickName;
    }

    @Data
    public static class MPArticleList {

        private String Title;
        private String Digest;
        private String Cover;
        private String Url;

    }

}