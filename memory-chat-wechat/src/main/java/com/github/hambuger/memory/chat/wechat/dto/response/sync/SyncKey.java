/**
 * Copyright 2021 bejson.com
 */
package com.github.hambuger.memory.chat.wechat.dto.response.sync;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

import lombok.Data;


/**
 * Auto-generated: 2021-02-22 13:35:59
 *
 * @author bejson.com (i@bejson.com)
 * @website http://www.bejson.com/java2pojo/
 */
@Data
public class SyncKey {

    @JSONField(name="Count")
    private int Count;

    @JSONField(name="List")
    private List<com.github.hambuger.memory.chat.wechat.dto.response.sync.List> List;


}