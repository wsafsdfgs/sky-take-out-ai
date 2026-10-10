package com.sky.aicustomerservice.context;

public class UserContext {
        private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
        private static final ThreadLocal<String> TOKEN = new ThreadLocal<>();

        public static void setUserId(Long userId){ USER_ID.set(userId); }
        public static Long getUserId(){ return USER_ID.get(); }

        // 【新增】存原始token，透传给主项目
        public static void setToken(String token){ TOKEN.set(token); }
        public static String getToken(){ return TOKEN.get(); }

        public static void clear(){
            USER_ID.remove();
            TOKEN.remove();   // 【新增】一并清理
        }
    }


