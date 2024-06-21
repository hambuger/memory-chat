package com.github.hambuger.memory.chat.wechat.dto.request;

import com.alibaba.fastjson.annotation.JSONField;
import com.github.hambuger.memory.chat.wechat.dto.response.sync.SyncKey;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class WxSyncReq {
    @JSONField(name = "BaseRequest")
    private BaseRequest BaseRequest;

    @JSONField(name = "SyncKey")
    private SyncKey SyncKey;

    @JSONField(name = "rr")
    private Long rr;
}
