package com.github.hambuger.memory.chat.wechat.dto.request;

import com.alibaba.fastjson.annotation.JSONField;

import lombok.Data;


@Data
public class WxInitReq {
    @JSONField(name ="BaseRequest")
    private BaseRequest baseRequest;
}
