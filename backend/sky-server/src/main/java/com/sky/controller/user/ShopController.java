package com.sky.controller.user;

import com.sky.result.Result;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("userShopController")
@RequestMapping("/user/shop")

@Slf4j
public class ShopController {
    public static final String KEY = "SHOP_Status";
    @Autowired
    private RedisTemplate redisTemplate;
    //查询店铺状态接口
    @GetMapping("/status")
    @Operation(summary = "查询店铺状态")
    public Result<Integer> getStatus() {
        Integer status=(Integer) redisTemplate.opsForValue().get(KEY);
        log.info("查询店铺状态为{}",  status == 1 ? "营业中" : "打烊中");
        return Result.success(status);
    }
}
