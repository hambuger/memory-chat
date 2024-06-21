package com.github.hambuger.memory.chat.wechat.dto.request.msg.send;

import com.github.hambuger.memory.chat.wechat.constant.WxReqParamsConstant;


/**
 * @作者 Hamburger
 * @项目 AutoWeChat
 * @创建时间 3/10/2021 2:49 PM
 * <p>
 * APP消息
 */

public class WebWXSendingAppMsg extends WebWXSendingMsg {

    public WebWXSendingAppMsg() {
        super(WxReqParamsConstant.WXSendMsgCodeEnum.APP.getCode());
    }
}
