package com.bookstore.controller.shop;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bookstore.common.ErrorCode;
import com.bookstore.common.Result;
import com.bookstore.entity.AfterSale;
import com.bookstore.exception.BusinessException;
import com.bookstore.service.AfterSaleService;
import com.bookstore.service.ShopService;
import com.bookstore.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shop/after-sale")
public class ShopAfterSaleController {

    @Autowired
    private AfterSaleService afterSaleService;

    @Autowired
    private ShopService shopService;

    // 商家查看自己店铺的售后列表
    @GetMapping("/page")
    public Result<IPage<AfterSale>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        Long userId = UserContext.getUserId();
        Long shopId = shopService.getShopIdByUserId(userId);
        if (shopId == null) throw new BusinessException(ErrorCode.SHOP_NOT_OWNED);
        IPage<AfterSale> result = afterSaleService.getShopAfterSales(shopId, page, size, status);
        return Result.success(result);
    }

    // 商家审核
    @PutMapping("/review/{id}")
    public Result<Void> review(
            @PathVariable Long id,
            @RequestParam Integer status,
            @RequestParam(required = false) String remark) {
        Long userId = UserContext.getUserId();
        Long shopId = shopService.getShopIdByUserId(userId);
        if (shopId == null) throw new BusinessException(ErrorCode.SHOP_NOT_OWNED);
        afterSaleService.shopReview(id, shopId, status, remark);
        return Result.success();
    }
}