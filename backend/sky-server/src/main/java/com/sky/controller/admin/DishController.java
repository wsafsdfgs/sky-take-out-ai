package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
// 菜品管理
@RequestMapping("/admin/dish")
@Slf4j
@Tag(name = "菜品管理相关接口")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;
    @PostMapping
    @Operation(summary = "新增菜品")
    //新增菜品
    public Result save(@RequestBody DishDTO dishDTO){
        log.info("新增菜品:{}",dishDTO);
        dishService.saveWithFlavor(dishDTO);
        //清理缓存数据
        String key ="dish_"+dishDTO.getCategoryId();
        cleanCache(key);
        return Result.success();
    }
    @GetMapping("/page")
    @Operation(summary = "分页查询菜品")
    //分页查询菜品
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){
        log.info("分页查询菜品:{}",dishPageQueryDTO);
       PageResult pageResult= dishService.pageQuery(dishPageQueryDTO);

        return Result.success(pageResult);
    }
    @DeleteMapping
    @Operation(summary = "删除菜品")
    //删除菜品
    public Result delete(@RequestParam List<Long> ids){
        log.info("删除菜品:{}",ids);
        dishService.deleteBeatch(ids);

        //将所有的菜品缓存数据清理掉，所有以dish_开头的key
        cleanCache("dish_*");
        return Result.success();
    }
   @GetMapping("/{id}")
   @Operation(summary = "根据id查询菜品")
    //根据id查询
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("根据id查询菜品:{}",id);
        DishVO dishVO= dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }
    @PutMapping
    @Operation(summary = "修改菜品")
    //修改菜品
    public Result update(@RequestBody DishDTO dishDTO){
        log.info("修改菜品:{}",dishDTO);
        dishService.updateWithFlavor(dishDTO);

        //将所有的菜品缓存数据清理掉，所有以dish_开头的key
        cleanCache("dish_*");

        return Result.success();
    }
    @PostMapping("/status/{status}")
    @Operation(summary = "菜品起售停售")
    //修改菜品状态
    public Result<String> startOrStop(@PathVariable Integer status, Long id){
        log.info("修改菜品状态:{}",status);
        dishService.startOrStop(status,id);

        //清理缓存数据
        cleanCache("dish_*");

        return Result.success();
    }
    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @Operation(summary = "根据分类id查询菜品")
    public Result<List<Dish>> list(Long categoryId){
        List<Dish> list = dishService.list(categoryId);
        return Result.success(list);
    }
/**
 * 清除Redis中匹配指定模式的缓存键
 * @param pattern 缓存键的匹配模式，可以使用通配符，例如"user:*"表示所有以"user:"开头的键
 */
    private void cleanCache(String pattern){
    // 根据pattern获取所有匹配的键集合
        Set keys = redisTemplate.keys(pattern);
    // 删除获取到的所有键
        redisTemplate.delete(keys);
    }

}
