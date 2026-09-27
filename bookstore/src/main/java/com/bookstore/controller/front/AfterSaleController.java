package com.bookstore.controller.front;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bookstore.common.ErrorCode;
import com.bookstore.common.Result;
import com.bookstore.dto.AfterSaleApplyDTO;
import com.bookstore.exception.BusinessException;
import com.bookstore.entity.AfterSale;
import com.bookstore.service.AfterSaleService;
import com.bookstore.util.UserContext;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/after-sale")
public class AfterSaleController {

    @Autowired
    private AfterSaleService afterSaleService;

    // 用户申请售后
    @PostMapping("/apply")
    public Result<Void> apply(@Valid @RequestBody AfterSaleApplyDTO dto) {
        Long userId = UserContext.getUserId();
        afterSaleService.apply(userId, dto);
        return Result.success();
    }

    // 用户查看自己的售后列表
    @GetMapping("/user/list")
    public Result<IPage<AfterSale>> userList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status) {
        Long userId = UserContext.getUserId();
        IPage<AfterSale> result = afterSaleService.getUserAfterSales(userId, page, size, status);
        return Result.success(result);
    }

    // 查询售后单详情
    @GetMapping("/{id}")
    public Result<AfterSale> detail(@PathVariable Long id) {
        AfterSale as = afterSaleService.getDetail(id);
        // 权限校验：只能查看自己的
        if (!as.getUserId().equals(UserContext.getUserId())) {
            throw new BusinessException(ErrorCode.AFTER_SALE_NO_PERMISSION);
        }
        return Result.success(as);
    }
}