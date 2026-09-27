package com.bookstore.service.impl;

import com.bookstore.mapper.BookMapper;
import com.bookstore.mapper.OrderMapper;
import com.bookstore.mapper.ShopMapper;
import com.bookstore.service.ShopStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ShopStatisticsServiceImpl implements ShopStatisticsService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Override
    public Map<String, Object> getShopOverview(Long userId) {
        // 获取店铺ID
        Long shopId = shopMapper.getShopIdByUserId(userId);
        if (shopId == null) return new HashMap<>();

        Map<String, Object> result = new HashMap<>();
        // 总销售额
        BigDecimal totalSales = orderMapper.getShopTotalSales(shopId);
        result.put("totalSales", totalSales != null ? totalSales : BigDecimal.ZERO);
        // 总订单数
        Long totalOrders = orderMapper.countShopOrders(shopId);
        result.put("totalOrders", totalOrders != null ? totalOrders : 0L);
        // 库存预警数量
        Integer lowStockCount = bookMapper.getShopLowStockCount(shopId);
        result.put("lowStockCount", lowStockCount != null ? lowStockCount : 0);
        // 商品总数
        Long totalBooks = bookMapper.countShopBooks(shopId);
        result.put("totalBooks", totalBooks != null ? totalBooks : 0L);
        return result;
    }

    @Override
    public List<Map<String, Object>> getHotBooks(Long userId, int limit) {
        Long shopId = shopMapper.getShopIdByUserId(userId);
        if (shopId == null) return List.of();
        return bookMapper.getShopHotBooks(shopId, limit);
    }

    @Override
    public List<Map<String, Object>> getStockWarning(Long userId) {
        Long shopId = shopMapper.getShopIdByUserId(userId);
        if (shopId == null) return List.of();
        return bookMapper.getShopStockWarning(shopId);
    }

    @Override
    public Map<String, Object> getTodaySales(Long userId) {
        Long shopId = shopMapper.getShopIdByUserId(userId);
        if (shopId == null) return new HashMap<>();

        Map<String, Object> result = new HashMap<>();
        // 今日成交金额
        BigDecimal todayAmount = orderMapper.getShopTodaySales(shopId);
        result.put("todayAmount", todayAmount != null ? todayAmount : BigDecimal.ZERO);
        // 今日订单数
        Long todayOrders = orderMapper.countShopTodayOrders(shopId);
        result.put("todayOrders", todayOrders != null ? todayOrders : 0L);
        return result;
    }
}