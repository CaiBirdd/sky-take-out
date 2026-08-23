package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Api(tags = "店铺相关接口")
@Slf4j
public class ShopController {
    public  static final  String  KEY = "SHOP_STATUS_";
    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 设置店铺的营业状态
     * @param status 店铺状态，0表示关闭，1表示开启
     */
    @PutMapping("/{status}")
    @ApiOperation("设置店铺的营业状态")
    public Result setStatus(@PathVariable Integer status) {
        log.info("设置店铺的营业状态为：{}", status == 1 ? "营业中" : "打烊中");
        // 将店铺状态存入Redis中，key为"SHOP_STATUS"，value为status
        redisTemplate.opsForValue().set(KEY, status);
        return Result.success();
    }
    /**
     * 获取店铺的营业状态
     * @return 店铺状态，0表示关闭，1表示开启
     */
    @GetMapping("/status")
    @ApiOperation("获取店铺的营业状态")
    public Result<Integer> getStatus() {
        // 从Redis中获取店铺状态
        Integer status = (Integer) redisTemplate.opsForValue().get(KEY);
        log.info("获取店铺的营业状态为：{}", status == 1 ? "营业中" : "打烊中");
        return Result.success(status);
    }
}
