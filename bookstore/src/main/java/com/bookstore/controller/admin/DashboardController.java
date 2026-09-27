package com.bookstore.controller.admin;

import com.bookstore.common.Result;
import com.bookstore.mapper.BookMapper;
import com.bookstore.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/dashboard")
public class DashboardController {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private BookMapper bookMapper;

    // 统计概览
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> result = new HashMap<>();
        result.put("totalSales", orderMapper.getTotalSales());
        result.put("totalOrders", orderMapper.selectCount(null));
        result.put("totalBooks", bookMapper.selectCount(null));
        result.put("lowStockCount", bookMapper.getLowStockCount());
        return Result.success(result);
    }

    // 月度销售额趋势
    @GetMapping("/trend")
    public Result<Map<String, Object>> trend(@RequestParam(defaultValue = "2026") Integer year) {
        List<Map<String, Object>> data = orderMapper.getMonthlyTrend(year);
        Map<String, Object> result = new HashMap<>();
        result.put("months", data.stream().map(m -> m.get("month")).toArray());
        result.put("values", data.stream().map(m -> m.get("total")).toArray());
        return Result.success(result);
    }

    // 分类销量占比
    @GetMapping("/category-ratio")
    public Result<List<Map<String, Object>>> categoryRatio() {
        return Result.success(orderMapper.getCategoryRatio());
    }

    // 库存预警 Top10
    @GetMapping("/stock-warning")
    public Result<List<Map<String, Object>>> stockWarning() {
        return Result.success(bookMapper.getStockWarning());
    }
}