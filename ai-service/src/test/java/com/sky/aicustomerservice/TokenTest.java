package com.sky.aicustomerservice;

import com.sky.aicustomerservice.util.JwtUtil;
import java.util.HashMap;
import java.util.Map;

public class TokenTest {
    public static void main(String[] args) {
        String secret = "itheima-itheima-itheima-itheima-itheima";

        Map<String, Object> cA = new HashMap<>();
        cA.put("userId", 4);
        String tokenA = JwtUtil.createJWT(secret, 7200000L, cA);
        System.out.println("A token (userId=4): " + tokenA);
    }
}
