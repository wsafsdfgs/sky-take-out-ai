package com.sky.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "员工修改密码时传递的数据模型")
public class EmployeeFixPwdDTO implements Serializable {

    @Schema(description = "旧密码")
    private String oldPwd;

    @Schema(description = "新密码")
    private String newPwd;
}
