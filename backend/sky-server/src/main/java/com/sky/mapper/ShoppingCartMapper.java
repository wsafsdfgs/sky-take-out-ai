package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {
/**
 * 根据购物车信息获取购物车列表
 * @param shoppingCart 购物车对象，包含查询条件
 * @return 返回购物车列表，包含符合条件的所有购物车信息
 */
List<ShoppingCart> list( ShoppingCart shoppingCart);
/**
 * 更新购物车中商品数量的方法
 * 通过商品ID更新购物车中对应商品的数量
 * @param cart 包含购物车ID和更新后数量的购物车对象
 */
  @Update("update shopping_cart set number = #{number} where id = #{id}")
    void updateNumberById(ShoppingCart cart);

/**
 * 向购物车表中插入一条购物车记录
 * @param shoppingCart 购物车对象，包含用户ID、菜品ID、套餐ID、菜品口味、名称、图片、数量、金额和创建时间等信息
 */
  @Insert("insert into shopping_cart " +
        "(user_id, dish_id, setmeal_id, dish_flavor, name, image, number, amount, create_time) " + // SQL插入语句，指定了要插入的字段
        "values (#{userId}, #{dishId}, #{setmealId}, #{dishFlavor}, #{name}, #{image}, #{number}, #{amount}, #{createTime})") // SQL插入语句的值部分，对应前面的字段
  void insert(ShoppingCart shoppingCart); 
  // 方法：按用户id清空该用户全部购物车
  @Delete("delete from shopping_cart where user_id = #{userId}")
  void DeleteByUserId(Long userId);

/**
 * 按购物车主键删除单条记录（sub 减少到 0 时删除该条）
 * @param id 购物车主键id
 */
@Delete("delete from shopping_cart where id = #{id}")
void deleteById(Long id);

/**
 * 批量插入购物车信息的方法
 *
 * @param shoppingCartList 购物车对象列表，包含需要批量插入的购物车信息
 * 该方法用于一次性将多个购物车记录插入到数据库中，
 * 相比逐条插入，批量插入可以提高数据库操作效率
 */
    void insertBatch(List<ShoppingCart> shoppingCartList);
}
