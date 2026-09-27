package com.bookstore.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bookstore.entity.Address;
import com.bookstore.vo.AddressVO;

import java.util.List;

public interface AddressService extends IService<Address> {

    /**
     * 获取用户的地址列表
     */
    List<AddressVO> getUserAddresses(Long userId);

    /**
     * 保存或更新地址（如果已存在则更新，否则新增）
     */
    void saveOrUpdateAddress(Long userId, String receiverName, String receiverPhone, String detailAddress);
}