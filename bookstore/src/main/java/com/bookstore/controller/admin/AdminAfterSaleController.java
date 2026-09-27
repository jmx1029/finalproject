package com.bookstore.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookstore.common.Result;
import com.bookstore.entity.AfterSale;
import com.bookstore.service.AfterSaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/after-sale")
public class AdminAfterSaleController {

    @Autowired
    private AfterSaleService afterSaleService;

    // 管理员查询所有售后单（可按状态筛选）
    @GetMapping("/list")
    public Result<IPage<AfterSale>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<AfterSale> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(AfterSale::getStatus, status);
        if (orderId != null && !orderId.isEmpty()) {
            wrapper.eq(AfterSale::getOrderId, Long.parseLong(orderId)); // 或使用字符串模糊匹配
        }
        wrapper.orderByDesc(AfterSale::getCreateTime);
        IPage<AfterSale> result = afterSaleService.page(new Page<>(page, size), wrapper);
        return Result.success(result);
    }

    // 管理员仲裁
    @PutMapping("/arbitrate/{id}")
    public Result<Void> arbitrate(
            @PathVariable Long id,
            @RequestParam Integer status,
            @RequestParam(required = false) String remark) {
        // 在 Service 中放开限制，只检查是否已完结（4、5）
        afterSaleService.adminArbitrate(id, status, remark);
        return Result.success();
    }
}