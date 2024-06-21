/**
 * Copyright 2021 bejson.com
 */
package com.github.hambuger.memory.chat.wechat.dto.response.sync;

import com.github.hambuger.memory.chat.wechat.entity.Contacts;

import java.util.List;

import lombok.Data;


/**
 * Auto-generated: 2021-02-22 13:35:59
 *
 * @author bejson.com (i@bejson.com)
 * @website http://www.bejson.com/java2pojo/
 */
@Data
public class WebWxSyncResp {

    private com.github.hambuger.memory.chat.wechat.dto.response.BaseResponse BaseResponse;
    private int AddMsgCount;
    private List<AddMsgList> AddMsgList;
    private int ModContactCount;
    private List<Contacts> ModContactList;
    private int DelContactCount;
    private List<Contacts> DelContactList;
    private int ModChatRoomMemberCount;
   // private List<String> ModChatRoomMemberList;
    private Profile Profile;
    private int ContinueFlag;
    private SyncKey SyncKey;
    private String SKey;
    private SyncCheckKey SyncCheckKey;

}