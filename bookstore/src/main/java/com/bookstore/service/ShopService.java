package com.bookstore.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bookstore.dto.ShopUpdateDTO;
import com.bookstore.entity.Shop;
import com.bookstore.vo.ShopVO;

public interface ShopService extends IService<Shop> {
    /**
     * 获取用户店铺信息
     */
    ShopVO getShopByUserId(Long userId);
    void updateShop(Long userId, ShopUpdateDTO dto); // 新增
    /**
     * 创建店铺（审核通过后调用）
     */
    Shop createShop(Long userId, String shopName, String description,
                    String contactPerson, String contactPhone, String address);

    /**
     * 更新店铺信息
     */
    void updateShop(Shop shop);

    /**
     * 获取官方自营店
     */
    Shop getOfficialShop();

    Long getShopIdByUserId(Long userId);
}