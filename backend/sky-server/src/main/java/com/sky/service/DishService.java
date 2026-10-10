package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;
import org.springframework.stereotype.Service;

import java.util.List;


public interface DishService {
/**
 * 根据风味保存菜品信息
 * 该方法用于将带有风味信息的菜品数据保存到数据库中
 *
 * @param dishDTO 菜品数据传输对象，包含菜品的基本信息及风味特征
 */
    public void saveWithFlavor(DishDTO dishDTO);

/**
 * 分页查询菜品信息
 *
 * @param dishPageQueryDTO 菜品分页查询条件数据传输对象，包含分页参数和查询条件
 * @return PageResult 分页查询结果，包含菜品数据列表和分页信息
 */
    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

/**
 * 批量删除方法
 * 根据传入的ID列表批量删除对应的数据记录
 * 
 * @param ids 需要删除的数据ID列表，使用Long类型存储
 */
    void deleteBeatch(List<Long> ids);

/**
 * 根据ID查询菜品信息，并返回包含口味数据的菜品对象
 *
 * @param id 菜品的唯一标识ID
 * @return DishVO 包含菜品口味信息的视图对象
 */
    DishVO getByIdWithFlavor(Long id);

/**
 * 根据风味信息更新菜品信息
 * 该方法用于接收一个DishDTO对象，并根据其中的风味信息更新对应的菜品数据
 *
 * @param dishDTO 包含风味信息的菜品数据传输对象，用于更新菜品信息
 */
    void updateWithFlavor(DishDTO dishDTO);

/**
 * 启动或停止某个功能/服务的方法
 * @param status 状态标识，用于决定是启动还是停止
 *               - 通常可以使用特定值表示启动（如1）和停止（如0）
 * @param id     需要操作的功能/服务的唯一标识符
 *               - 用于精确定位要操作的目标
 */
    void startOrStop(Integer status, Long id);
    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */

    List<Dish> list(Long categoryId);
    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    List<DishVO> listWithFlavor(Dish dish);
}
