package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EmployeeMapper {

    /**
     * 根据用户名查询员工
     * @param username
     * @return
     */
    @Select("select * from employee where username = #{username}")
    Employee getByUsername(String username);
    @Insert("insert into employee" +
            "(name, username, password, phone, sex, id_number, status, create_time, update_time, create_user, update_user) " +
            "values(#{name}, #{username}, #{password}, #{phone}, #{sex}, #{idNumber}, #{status}, now(), now(), #{createUser}, #{updateUser})")
    @AutoFill(value = OperationType.INSERT)
    void insert(Employee employee);

/**
 * 分页查询员工信息
 *
 * @param employeePageQueryDTO 员工分页查询条件数据传输对象，包含分页参数和查询条件
 * @return Page<Employee> 返回分页查询结果，包含员工数据列表和分页信息
 */
    Page<Employee>  pageQuery(EmployeePageQueryDTO employeePageQueryDTO);

/**
 * 更新员工信息的方法
 * @param employee 需要更新的员工对象，包含完整的员工信息
 */
    @AutoFill(value = OperationType.UPDATE)
    void update(Employee employee);
    //根据id查询员工信息
    @Select("select * from employee where id = #{id}")
    Employee getById(Long id);

    //根据id删除员工
    @Delete("delete from employee where id = #{id}")
    void deleteById(Long id);

    void update(EmployeeDTO employeeDTO);

}
