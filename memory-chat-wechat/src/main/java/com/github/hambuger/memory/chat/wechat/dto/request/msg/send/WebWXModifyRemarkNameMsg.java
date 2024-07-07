package com.github.hambuger.memory.chat.wechat.dto.request.msg.send;

import com.github.hambuger.memory.chat.wechat.dto.request.BaseRequest;

/**
 * @作者 Hamburger
 * @项目 AutoWeChat
 * @创建时间 3/10/2021 2:49 PM
 * <p>
 * 文本消息
 */

public class WebWXModifyRemarkNameMsg  extends WebWXSendingMsg {
   public com.github.hambuger.memory.chat.wechat.dto.request.BaseRequest BaseRequest = new BaseRequest();
   public Byte CmdId;
   public String RemarkName;
   public String UserName;
}
