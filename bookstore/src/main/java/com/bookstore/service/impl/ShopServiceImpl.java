package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bookstore.common.ErrorCode;
import com.bookstore.dto.ShopUpdateDTO;
import com.bookstore.entity.Shop;
import com.bookstore.exception.BusinessException;
import com.bookstore.mapper.ShopMapper;
import com.bookstore.service.ShopService;
import com.bookstore.vo.ShopVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements ShopService {

    @Override
    public ShopVO getShopByUserId(Long userId) {
        Shop shop = this.getOne(new LambdaQueryWrapper<Shop>()
                .eq(Shop::getUserId, userId)
                .eq(Shop::getIsDeleted, 0));
        if (shop == null) return null;
        ShopVO vo = new ShopVO();
        BeanUtils.copyProperties(shop, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Shop createShop(Long userId, String shopName, String description,
                           String contactPerson, String contactPhone, String address) {
        Shop shop = new Shop();
        shop.setUserId(userId);
        shop.setName(shopName);
        shop.setDescription(description);
        shop.setContactPerson(contactPerson);
        shop.setContactPhone(contactPhone);
        shop.setAddress(address);
        shop.setStatus(1);
        this.save(shop);
        return shop;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateShop(Shop shop) {
        this.updateById(shop);
    }

    @Override
    public Shop getOfficialShop() {
        return this.getOne(new LambdaQueryWrapper<Shop>()
                .eq(Shop::getName, "阅微书城官方自营店")
                .eq(Shop::getIsDeleted, 0));
    }

    @Autowired
    private ShopMapper shopMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateShop(Long userId, ShopUpdateDTO dto) {
        // 查询店铺
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Shop::getUserId, userId);
        Shop shop = this.getOne(wrapper);
        if (shop == null) {
            throw new BusinessException(ErrorCode.SHOP_NOT_OWNED);
        }

        // 更新字段
        if (StringUtils.hasText(dto.getName())) {
            shop.setName(dto.getName());
        }
        if (StringUtils.hasText(dto.getLogo())) {
            shop.setLogo(dto.getLogo());
        }
        if (StringUtils.hasText(dto.getDescription())) {
            shop.setDescription(dto.getDescription());
        }
        if (StringUtils.hasText(dto.getContactPerson())) {
            shop.setContactPerson(dto.getContactPerson());
        }
        if (StringUtils.hasText(dto.getContactPhone())) {
            shop.setContactPhone(dto.getContactPhone());
        }
        if (StringUtils.hasText(dto.getAddress())) {
            shop.setAddress(dto.getAddress());
        }
        // ===== 新增：更新公告 =====
        if (dto.getAnnouncement() != null) {
            shop.setAnnouncement(dto.getAnnouncement());
        }

        this.updateById(shop);
    }

    @Override
    public Long getShopIdByUserId(Long userId) {
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Shop::getUserId, userId).eq(Shop::getIsDeleted, 0);
        Shop shop = this.getOne(wrapper);
        return shop != null ? shop.getId() : null;
    }



}