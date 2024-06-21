package com.github.hambuger.memory.chat.wechat.dto.request.msg.send;

import com.github.hambuger.memory.chat.wechat.constant.WxReqParamsConstant;


/**
 * @作者 Hamburger
 * @项目 AutoWeChat
 * @创建时间 3/10/2021 2:49 PM
 * <p>
 * 图片消息
 */

public class WebWXSendingEmotionMsg extends WebWXSendingMsg {
    public Integer EmojiFlag = null;
    public String EMoticonMd5;

    public WebWXSendingEmotionMsg() {
        super(WxReqParamsConstant.WXSendMsgCodeEnum.EMOTION.getCode());
        super.Content = null;
    }
}
