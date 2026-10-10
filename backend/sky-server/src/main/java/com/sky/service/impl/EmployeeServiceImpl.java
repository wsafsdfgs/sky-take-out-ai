package com.sky.service.impl;


import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeFixPwdDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        //对前端传递的密码进行md5加密，然后再进行比对
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    @Override
    /**
     * 保存员工信息的方法
     * @param employeeDTO 包含员工数据的传输对象
     */
    public void save(EmployeeDTO employeeDTO) {
     // 创建Employee实体对象
     Employee employee = new Employee();
     // 将DTO对象的属性复制到Employee实体对象中
     BeanUtils.copyProperties(employeeDTO,employee);
     //设置账号状态，默认正常状态 1：正常 0：禁用
     employee.setStatus(StatusConstant.ENABLE);
     //设置密码，默认123456，需要MD5加密
     employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));
     //创建时间
     //employee.setCreateTime(LocalDateTime.now());
     //更新时间
     //employee.setUpdateTime(LocalDateTime.now());
     //创建人更新人
     //employee.setCreateUser(BaseContext.getCurrentId());
     //employee.setUpdateUser(BaseContext.getCurrentId());
     // 调用Mapper接口的方法保存员工信息
     employeeMapper.insert(employee);
    }

/**
 * 分页查询员工信息
 *
 * @param employeePageQueryDTO 员工分页查询条件数据传输对象
 * @return PageResult 分页查询结果，包含员工数据列表及分页信息
 */
@Override
    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        //开始分页查询
        PageHelper.startPage(employeePageQueryDTO.getPage(),employeePageQueryDTO.getPageSize());
        //调用Mapper接口的方法查询员工数据列表
        Page<Employee> page=employeeMapper.pageQuery(employeePageQueryDTO);
        long total=page.getTotal();
        List<Employee> records=page.getResult();
        return new PageResult(total,records);
    }
@Override
    public void startOrStop(Integer status, Long id) {
       Employee employee= Employee.builder()
               .status(status)
               .id(id)
               .build();
        employeeMapper.update(employee);    // 调用Mapper接口的方法，根据状态标识和ID更新员工信息
    }
    @Override
    public Employee getById(Long id) {
       Employee employee=employeeMapper.getById(id);
       employee.setPassword("******");
       return employee;
        }
        @Override
        public void update(EmployeeDTO employeeDTO) {
            Employee employee = new Employee();
            BeanUtils.copyProperties(employeeDTO,employee);
            //employee.setUpdateTime(LocalDateTime.now());
            //employee.setUpdateUser(BaseContext.getCurrentId());
            employeeMapper.update(employee);
        }

    /**
     * 员工注册（游客注册，尚未登录拿不到当前用户id，createUser/updateUser默认填100表示自己）
     * @param employeeLoginDTO
     */
    @Override
    public void register(EmployeeLoginDTO employeeLoginDTO) {
        // 密码MD5加密
        String password = employeeLoginDTO.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        employeeLoginDTO.setPassword(password);

        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeLoginDTO, employee);
        // 补齐默认字段（username取自DTO，name/phone/sex/idNumber给默认值）
        employee.setName("员工");
        employee.setPhone("11111111111");
        employee.setSex("1");
        employee.setIdNumber("000000000000000000");
        employee.setStatus(StatusConstant.ENABLE);
        employee.setCreateUser(100L);
        employee.setUpdateUser(100L);
        employeeMapper.insert(employee);
    }

    /**
     * 修改当前登录账号的密码
     * @param employeeFixPwdDTO
     */
    @Override
    public void fixPwd(EmployeeFixPwdDTO employeeFixPwdDTO) {
        // 旧密码MD5加密后与数据库比对
        String oldPwd = DigestUtils.md5DigestAsHex(employeeFixPwdDTO.getOldPwd().getBytes());
        Long id = BaseContext.getCurrentId();
        Employee employee = employeeMapper.getById(id);
        if (employee == null || !oldPwd.equals(employee.getPassword())) {
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }
        // 旧密码正确，新密码MD5加密后更新
        String newPwd = DigestUtils.md5DigestAsHex(employeeFixPwdDTO.getNewPwd().getBytes());
        employee.setPassword(newPwd);
        employeeMapper.update(employee);
    }

    /**
     * 根据id删除员工
     * @param id
     */
    @Override
    public void deleteById(Long id) {
        employeeMapper.deleteById(id);
    }
    }

