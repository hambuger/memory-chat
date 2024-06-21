package com.github.hambuger.memory.chat.wechat.dto.response;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class SyncCheckResp {
    private Integer retCode;
    private String selector;
}
