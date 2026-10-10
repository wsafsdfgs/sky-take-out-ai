package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Map;

@Mapper
public interface UserMapper {

    /**
     * 根据openid查询用户
     * @param openid
     * @return
     */
    @Select("select * from user where openid = #{openid}")
    User getByOpenid(String openid);

    /**
     * 插入数据
     * @param user
     */
    void insert(User user);
@Select("select * from user where id = #{id}")
    User getById(Long userId);


/**
 * 通过Map统计数量
 * @param map 输入的Map集合，用于统计数量
 * @return 返回统计后的数量结果，类型为Integer
 */
    Integer countByMap(Map map);
    /**
     * 更新用户信息（映射到 sex/avatar 列）
     * @param user
     */
    @Update("update user set name = #{name}, phone = #{phone}, sex = #{sex}, avatar = #{avatar} where id = #{id}")
    void update(User user);

}