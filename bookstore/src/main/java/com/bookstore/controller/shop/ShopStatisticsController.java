package com.bookstore.controller.shop;

import com.bookstore.common.Result;
import com.bookstore.service.ShopStatisticsService;
import com.bookstore.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/shop/statistics")
public class ShopStatisticsController {

    @Autowired
    private ShopStatisticsService statisticsService;

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Long userId = UserContext.getUserId();
        return Result.success(statisticsService.getShopOverview(userId));
    }

    @GetMapping("/hot-books")
    public Result<?> hotBooks(@RequestParam(defaultValue = "10") Integer limit) {
        Long userId = UserContext.getUserId();
        return Result.success(statisticsService.getHotBooks(userId, limit));
    }

    @GetMapping("/stock-warning")
    public Result<?> stockWarning() {
        Long userId = UserContext.getUserId();
        return Result.success(statisticsService.getStockWarning(userId));
    }

    @GetMapping("/today-sales")
    public Result<Map<String, Object>> todaySales() {
        Long userId = UserContext.getUserId();
        return Result.success(statisticsService.getTodaySales(userId));
    }
}