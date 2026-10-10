package com.sky.service;

import com.sky.dto.UserDTO;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.vo.UserVO;

public interface UserService {

    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */
    User wxLogin(UserLoginDTO userLoginDTO);
    /**
     * 根据id查询用户信息（转成对外VO，gender/pic）
     * @param id
     * @return
     */
    UserVO getUserById(Long id);

    /**
     * 更新用户信息（接收DTO的gender/pic，存sex/avatar）
     * @param userDTO
     */
    void updateUser(UserDTO userDTO);

}