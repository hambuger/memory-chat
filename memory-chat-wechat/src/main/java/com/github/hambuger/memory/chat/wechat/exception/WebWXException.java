package com.github.hambuger.memory.chat.wechat.exception;

/**
 * @作者 Hamburger
 * @项目 AutoWeChat
 * @创建时间 3/10/2021 2:03 PM
 */
public class WebWXException extends Exception {

    public WebWXException(String message) {
        super(message);
    }

    public WebWXException(String message, Throwable cause) {
        super(message, cause);
    }
}
