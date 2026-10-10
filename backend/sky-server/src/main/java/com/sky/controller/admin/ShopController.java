package com.sky.controller.admin;

import com.sky.result.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Tag(name = "店铺管理")
@Slf4j
public class ShopController {

    public static final String KEY = "SHOP_Status";

    @Autowired
    private RedisTemplate redisTemplate;
    @PutMapping("/{status}")
    @Operation(summary = "修改店铺状态")
    public Result setStatus(@PathVariable Integer status) {
        log.info("修改店铺状态，status：{}",status == 1 ? "营业中" : "打烊中");
        redisTemplate.opsForValue().set(KEY,status);
        return Result.success();
    }

    //查询店铺状态接口
    @GetMapping("/status")
    @Operation(summary = "查询店铺状态")
    public Result<Integer> getStatus() {
        Integer status=(Integer) redisTemplate.opsForValue().get(KEY);
        log.info("查询店铺状态为{}",status == 1 ? "营业中" : "打烊中");
        return Result.success(status);
    }
}
