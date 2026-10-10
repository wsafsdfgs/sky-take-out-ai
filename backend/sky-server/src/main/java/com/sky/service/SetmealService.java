package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealService {
    /**
     * 新增套餐，同时需要保存套餐和菜品的关联关系
     * @param setmealDTO
     */
    void saveWithDish(SetmealDTO setmealDTO);

/**
 * 分页查询套餐信息
 * 根据提供的查询条件进行套餐数据的分页查询
 *
 * @param setmealPageQueryDTO 套餐分页查询条件数据传输对象，包含分页参数和查询条件
 * @return PageResult 分页查询结果，包含当前页数据、总记录数等信息
 */
    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

/**
 * 批量删除方法
 * 根据提供的ID列表批量删除对应的数据记录
 *
 * @param ids 需要删除的数据ID列表，使用Long类型存储
 */
    void deleteBatch(List<Long> ids);

/**
 * 根据ID查询套餐信息，并包含关联的菜品数据
 *
 * @param id 套餐ID
 * @return 返回包含菜品信息的套餐视图对象(SetmealVO)
 */
    SetmealVO getByIdWithDish(Long id);

/**
 * 更新套餐信息的方法
 * @param setmealDTO 套餐数据传输对象，包含需要更新的套餐信息
 */
    void update(SetmealDTO setmealDTO);

    void startOrStop(Integer status, Long id);
    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    List<Setmeal> list(Setmeal setmeal);
    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    List<DishItemVO> getDishItemById(Long id);
}
