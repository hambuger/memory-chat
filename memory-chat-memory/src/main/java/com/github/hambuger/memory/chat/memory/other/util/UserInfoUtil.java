package com.github.hambuger.memory.chat.memory.other.util;

import java.util.*;


/**
 * @author hamburger
 * @since 2024/9/2
 */
public class UserInfoUtil {

    private static final InheritableThreadLocal<Map<String, String>> threadLocalMap = new InheritableThreadLocal<>() {
        @Override
        protected Map<String, String> initialValue() {
            return new HashMap<>();
        }


        @Override
        protected Map<String, String> childValue(Map<String, String> parentValue) {
            // The child thread will receive a copy of the parent thread and can optionally make a deep copy of it.
            return new HashMap<>(parentValue);
        }
    };


    public static void putUser(String userName) {
        threadLocalMap.get().put("user", userName);
    }


    public static String getUser() {
        return threadLocalMap.get().get("user");
    }

}
