package com.sky.service;

import com.sky.entity.AddressBook;
import java.util.List;

public interface AddressBookService {

/**
 * 根据通讯录对象查询并返回一个通讯录列表
 *
 * @param addressBook 通讯录对象，作为查询条件
 * @return 返回一个包含符合条件通讯录对象的列表
 */
    List<AddressBook> list(AddressBook addressBook);

/**
 * 保存地址簿的方法
 * @param addressBook 需要保存的地址簿对象
 */
    void save(AddressBook addressBook);

/**
 * 根据ID获取地址簿信息
 * @param id 地址簿的唯一标识符
 * @return 返回对应的地址簿对象，如果未找到则返回null
 */
    AddressBook getById(Long id);

/**
 * 更新地址簿的方法
 * @param addressBook 需要更新的地址簿对象
 */
    void update(AddressBook addressBook);

/**
 * 设置默认通讯录的方法
 * @param addressBook 要设置为默认的通讯录对象
 */
    void setDefault(AddressBook addressBook);

    void deleteById(Long id);

}
