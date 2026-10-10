package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {

    /**
     * 清空购物车的方法
     *
     * */
    void cLeanShoppingCart();

/**
 * 添加商品到购物车的方法
 * @param shoppingCartDTO 购物车数据传输对象，包含商品信息、数量等信息
 */
    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);
    List<ShoppingCart> showShoppingCart();

/**
* 将商品数量减一的方法
* @param shoppingCartDTO 购物车数据传输对象，包含商品信息、数量等信息
* */
    void subShoppingCart(ShoppingCartDTO shoppingCartDTO);
}
