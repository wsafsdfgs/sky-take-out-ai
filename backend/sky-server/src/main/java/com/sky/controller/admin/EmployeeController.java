package com.sky.controller.admin;

import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeFixPwdDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.mapper.EmployeeMapper;
import com.sky.properties.JwtProperties;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.EmployeeService;
import com.sky.utils.JwtUtil;
import com.sky.vo.EmployeeLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 员工管理
 */
@RestController
@RequestMapping("/admin/employee")
@Slf4j
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登录
     *
     * @param employeeLoginDTO
     * @return
     */
    @PostMapping("/login")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("员工登录：{}", employeeLoginDTO);

        Employee employee = employeeService.login(employeeLoginDTO);

        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(token)
                .build();

        return Result.success(employeeLoginVO);
    }

    /**
     * 退出
     *
     * @return
     */
    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public Result<String> logout() {
        return Result.success();
    }

    //新增员工
    @PostMapping
    public Result save(@RequestBody EmployeeDTO employeeDTO) {
        log.info("新增员工：{}", employeeDTO);
        employeeService.save(employeeDTO);
        return Result.success();
    }

    //新增员工（兼容路径 /add 写法）
    @PostMapping("/add")
    @Operation(summary = "新增员工(add写法)")
    public Result saveByAdd(@RequestBody EmployeeDTO employeeDTO) {
        log.info("新增员工(add写法)：{}", employeeDTO);
        employeeService.save(employeeDTO);
        return Result.success();
    }

    //分页查询
    @GetMapping("/page")
    @Operation(summary = "分页查询")
    public Result<PageResult> page(EmployeePageQueryDTO employeePageQueryDTO) {
        log.info("分页查询：{}", employeePageQueryDTO);
        PageResult pageResult = employeeService.pageQuery(employeePageQueryDTO);
        return Result.success(pageResult);
    }

    //启用禁用员工账号
    @PostMapping("/status/{status}")
    @Operation(summary = "启用禁用员工账号")
    public Result startOrStop(@PathVariable Integer status, Long id) {
        log.info("启用禁用员工账号：{},{}", status, id);
        employeeService.startOrStop(status, id);
        return Result.success();
    }

    //根据id查询员工信息
    @GetMapping("{id}")
    @Operation(summary = "根据id查询员工信息")
    public Result<Employee> getById(@PathVariable Long id) {
        Employee employee = employeeService.getById(id);
        log.info("员工信息：{}",employee);
        return Result.success(employee);
    }
    @PutMapping
    @Operation(summary = "编辑员工信息")
    public Result update(@RequestBody EmployeeDTO employeeDTO) {
        log.info("编辑员工信息：{}", employeeDTO);
        employeeService.update(employeeDTO);
        return Result.success();
    }

    //编辑员工信息（兼容路径 /update 写法）
    @PutMapping("/update")
    @Operation(summary = "编辑员工信息(update写法)")
    public Result updateByPath(@RequestBody EmployeeDTO employeeDTO) {
        log.info("编辑员工信息(update写法)：{}", employeeDTO);
        employeeService.update(employeeDTO);
        return Result.success();
    }

    //员工注册（与登录同格式DTO，只做新增，不签发token）
    @PostMapping("/register")
    @Operation(summary = "员工注册")
    public Result register(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("员工注册：{}", employeeLoginDTO.getUsername());
        employeeService.register(employeeLoginDTO);
        return Result.success();
    }

    //修改当前登录账号的密码
    @PutMapping("/fixpwd")
    @Operation(summary = "修改密码")
    public Result fixPwd(@RequestBody EmployeeFixPwdDTO employeeFixPwdDTO) {
        log.info("修改密码：{}", employeeFixPwdDTO);
        employeeService.fixPwd(employeeFixPwdDTO);
        return Result.success();
    }

    //根据id删除员工
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "根据id删除员工")
    public Result delete(@PathVariable Long id) {
        log.info("删除员工：{}", id);
        employeeService.deleteById(id);
        return Result.success();
    }

    //启用禁用员工账号（兼容路径参数为id的写法：PUT /status/{id}，与上方 POST /status/{status} 区分）
    @PutMapping("/status/{id}")
    @Operation(summary = "启用禁用员工账号(id写法)")
    public Result startOrStopById(@PathVariable Long id) {
        log.info("启用禁用员工账号(id写法)：{}", id);
        Employee employee = employeeService.getById(id);
        if (employee == null) {
            return Result.error("员工不存在");
        }
        Integer newStatus = employee.getStatus() == 1 ? 0 : 1;
        employeeService.startOrStop(newStatus, id);
        return Result.success();
    }

}
