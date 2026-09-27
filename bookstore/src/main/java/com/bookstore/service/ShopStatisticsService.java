package com.bookstore.service;

import java.util.List;
import java.util.Map;

public interface ShopStatisticsService {
    Map<String, Object> getShopOverview(Long userId);
    List<Map<String, Object>> getHotBooks(Long userId, int limit);
    List<Map<String, Object>> getStockWarning(Long userId);
    Map<String, Object> getTodaySales(Long userId);
}