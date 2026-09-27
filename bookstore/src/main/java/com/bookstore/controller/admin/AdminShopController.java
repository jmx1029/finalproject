package com.bookstore.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookstore.common.ErrorCode;
import com.bookstore.common.Result;
import com.bookstore.entity.Shop;
import com.bookstore.exception.BusinessException;
import com.bookstore.entity.ShopApplication;
import com.bookstore.entity.User;
import com.bookstore.mapper.OrderMapper;
import com.bookstore.service.ShopApplicationService;
import com.bookstore.service.ShopService;
import com.bookstore.service.UserService;
import com.bookstore.vo.ShopAdminVO;
import com.bookstore.vo.ShopApplicationVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/shop")
public class AdminShopController {

    @Autowired
    private ShopApplicationService shopApplicationService;

    @Autowired
    private ShopService shopService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrderMapper orderMapper;

    // ============================================================
    //  1. 商家申请审核（原有功能，保留）
    // ============================================================

    @GetMapping("/applications")
    public Result<IPage<ShopApplicationVO>> listApplications(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        return Result.success(shopApplicationService.getAllApplications(page, size, status));
    }

    @GetMapping("/applications/pending")
    public Result<IPage<ShopApplicationVO>> getPendingApplications(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(shopApplicationService.getPendingApplications(page, size));
    }

    @PutMapping("/applications/{id}/review")
    public Result<Void> reviewApplication(
            @PathVariable Long id,
            @RequestBody Map<String, Object> params) {
        Integer status = (Integer) params.get("status");
        String remark = (String) params.get("remark");
        shopApplicationService.reviewApplication(id, status, remark);
        return Result.success();
    }

    // ============================================================
    //  2. 已入驻商家管理（新增 + 补全）
    // ============================================================

    /**
     * 商家列表（分页 + 关键词搜索）
     */
    @GetMapping("/page")
    public Result<IPage<ShopAdminVO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {

        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Shop::getIsDeleted, 0);

        // 关键词搜索：店铺名称 / 联系人 / 联系电话
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Shop::getName, keyword)
                    .or().like(Shop::getContactPerson, keyword)
                    .or().like(Shop::getContactPhone, keyword));
        }
        if (status != null) {
            wrapper.eq(Shop::getStatus, status);
        }
        wrapper.orderByDesc(Shop::getCreateTime);

        IPage<Shop> shopPage = shopService.page(new Page<>(page, size), wrapper);
        IPage<ShopAdminVO> voPage = new Page<>(page, size);
        voPage.setTotal(shopPage.getTotal());

        List<ShopAdminVO> voList = new ArrayList<>();
        for (Shop shop : shopPage.getRecords()) {
            ShopAdminVO vo = new ShopAdminVO();
            BeanUtils.copyProperties(shop, vo);

            // 查询店主信息
            User owner = userService.getById(shop.getUserId());
            if (owner != null) {
                vo.setOwnerUsername(owner.getUsername());
                vo.setOwnerNickname(owner.getNickname());
            }

            // 统计商品数量（tb_book）
            // 统计订单数量（tb_order）
            // 可在 Service 层优化，这里简单处理
            voList.add(vo);
        }
        voPage.setRecords(voList);
        return Result.success(voPage);
    }

    /**
     * 商家详情
     */
    @GetMapping("/{id}")
    public Result<ShopAdminVO> detail(@PathVariable Long id) {
        Shop shop = shopService.getById(id);
        if (shop == null) {
            throw new BusinessException(ErrorCode.SHOP_APPLY_NOT_FOUND);
        }

        ShopAdminVO vo = new ShopAdminVO();
        BeanUtils.copyProperties(shop, vo);

        User owner = userService.getById(shop.getUserId());
        if (owner != null) {
            vo.setOwnerUsername(owner.getUsername());
            vo.setOwnerNickname(owner.getNickname());
        }

        return Result.success(vo);
    }

    /**
     * 禁用/启用商家（原有，保留）
     */
    @PutMapping("/{id}/status")
    public Result<Void> toggleShopStatus(@PathVariable Long id, @RequestParam Integer status) {
        Shop shop = shopService.getById(id);
        if (shop == null) {
            throw new BusinessException(ErrorCode.SHOP_APPLY_NOT_FOUND);
        }
        shop.setStatus(status);
        shopService.updateById(shop);
        return Result.success();
    }
}