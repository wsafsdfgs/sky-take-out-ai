package com.sky.service;

import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeFixPwdDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.result.PageResult;

public interface EmployeeService {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    /**
     * 新增员工
     * @param employeeDTO
     */
    void save(EmployeeDTO employeeDTO);

/**
 * 分页查询员工信息的方法
 * @param employeePageQueryDTO 员工分页查询条件对象，包含分页参数和查询条件
 * @return PageResult 分页查询结果对象，包含分页数据和总数等信息
 */
    PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO);

/**
 * 启动或停止某个功能的方法
 * @param status 状态参数，用于决定是启动还是停止，具体值含义需要根据业务逻辑确定
 * @param id 需要操作的对象的唯一标识符
 */
    void startOrStop(Integer status, Long id);

/**
 * 根据员工ID获取员工信息的方法
 * @param id 员工的唯一标识符，Long类型
 * @return 返回一个Employee对象，包含对应ID的员工详细信息
 *         如果未找到对应ID的员工，可能返回null
 */
    Employee getById(Long id);

/**
 * 更新员工信息的方法
 * @param employeeDTO 包含员工更新数据的DTO对象
 */
    void update(EmployeeDTO employeeDTO);

    /**
     * 员工注册（游客注册，createUser默认100）
     * @param employeeLoginDTO
     */
    void register(EmployeeLoginDTO employeeLoginDTO);

    /**
     * 修改当前登录账号的密码
     * @param employeeFixPwdDTO
     */
    void fixPwd(EmployeeFixPwdDTO employeeFixPwdDTO);

    /**
     * 根据id删除员工
     * @param id
     */
    void deleteById(Long id);
}
