package com.github.hambuger.memory.chat.wechat.dto.request;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class WxCreateRoomReq {

    @JSONField(name = "MemberCount")
    private Integer	MemberCount;

    @JSONField(name = "MemberList")
    private List<NewRoomMember> MemberList;

    @JSONField(name = "Topic")
    private String	Topic;

    @JSONField(name = "BaseRequest")
    private BaseRequest	BaseRequest;
    @Data
    @Builder
    public static class NewRoomMember{
        @JSONField(name = "UserName")
        String UserName;
    }
}
