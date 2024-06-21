package com.github.hambuger.memory.chat.wechat.dto.response;

import com.github.hambuger.memory.chat.wechat.entity.Contacts;

import java.util.List;

import lombok.Data;


@Data
public class WxCreateRoomResp {


    private BaseResponse	BaseResponse;

    private String	Topic;


    private String	PYInitial;


    private String	QuanPin;


    private Integer	MemberCount;


    private List<Contacts> MemberList;


    private String	ChatRoomName;


    private String	BlackList;
}
