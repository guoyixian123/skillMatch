package com.skillmatch.context;

import org.springframework.stereotype.Component;

@Component
public class BeanContext {
    private static final ThreadLocal<String> tl = new ThreadLocal<>();
    public static String getUserId() {
       return tl.get();
    }
    public static void setUserId(String userId) {
        tl.set(userId);
    }
    public static void remove() {
        tl.remove();
    }
}
