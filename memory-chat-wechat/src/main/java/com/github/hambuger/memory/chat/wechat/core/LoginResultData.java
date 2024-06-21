package com.github.hambuger.memory.chat.wechat.core;

import com.alibaba.fastjson.annotation.JSONField;
import com.github.hambuger.memory.chat.wechat.dto.request.BaseRequest;
import com.github.hambuger.memory.chat.wechat.dto.response.sync.SyncCheckKey;
import com.github.hambuger.memory.chat.wechat.dto.response.sync.SyncKey;

import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class LoginResultData{
        private String url;
        private String fileUrl;
        private String syncUrl;
        private String deviceId;
        private Integer inviteStartCount;
        private SyncKey syncKeyObject;
        private String syncKey;
        private SyncCheckKey syncCheckKey;
        @JSONField(name ="pass_ticket")
        private String passTicket;
        @JSONField(name ="BaseRequest")
        private BaseRequest baseRequest;



    public void setSyncKeyObject(SyncKey syncKeyObject) {
        this.syncKey = syncKeyObject.getList()
                .stream()
                .map(e -> e.getKey() + "_" + e.getVal()).collect(Collectors.joining("|"));
        this.syncKeyObject = syncKeyObject;
    }
}