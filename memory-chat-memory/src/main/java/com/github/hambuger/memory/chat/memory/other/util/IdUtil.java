package com.github.hambuger.memory.chat.memory.other.util;

import java.util.Date;
import java.util.Random;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;


/**
 * @author hamburger
 * @since 2024/6/14
 */
public class IdUtil {

    public static String generateUniqueId() {
        return "MID" + DateUtil.format(new Date(), DatePattern.PURE_DATETIME_MS_PATTERN + new Random().nextInt(100));
    }

}
