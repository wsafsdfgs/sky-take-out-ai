package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DishMapper {
/**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

/**
 * 插入菜品的方法
 * 使用了AutoFill注解，表示在执行INSERT类型操作时自动填充某些字段
 * @param dish 要插入的菜品对象，包含菜品的相关信息
 */
@AutoFill(value = OperationType.INSERT)
    void insert(Dish dish);

/**
 * 分页查询菜品信息
 * 根据提供的查询条件分页返回菜品视图对象(DishVO)列表
 *
 * @param dishPageQueryDTO 菜品分页查询条件数据传输对象，包含分页参数和查询过滤条件
 * @return Page<DishVO> 分页查询结果，包含菜品视图对象列表及分页信息
 */
    Page<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO);
    @Select("select * from dish where id = #{id}")
    Dish getById(Long id);

/**
 * 根据ID删除菜品
 * @param id 菜品的ID，用于标识要删除的菜品
 * 使用@Delete注解执行SQL删除语句，从dish表中删除指定ID的记录
 */
    @Delete("delete from dish where id = #{id}")
    void deleteById(Long id);

/**
 * 根据ID列表批量删除记录
 * @param ids 要删除的记录ID列表
 */
    void deleteByIds(List<Long> ids);

/**
 * 更新菜品信息的方法
 * @param dish 需要更新的Dish对象，包含新的菜品信息
 */
    @AutoFill(value = OperationType.UPDATE)
    void update(Dish dish); // 方法声明，用于更新传入的Dish对象的信息
    /**
     * 动态条件查询菜品
     * @param dish
     * @return
     */

    List<Dish> list(Dish dish);

    /**
     * 根据套餐id查询菜品
     * @param setmealId
     * @return
     */
    @Select("select a.* from dish a left join setmeal_dish b on a.id = b.dish_id where b.setmeal_id = #{setmealId}")
    List<Dish> getBySetmealId(Long setmealId);

        /**
         * 根据条件统计菜品数量
         * @param map
         * @return
         */
        Integer countByMap(Map map);
}


