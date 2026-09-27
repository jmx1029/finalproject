package com.bookstore.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderStatisticsDTO {
    // 统计卡片
    private Long totalOrders;          // 总订单数
    private BigDecimal totalSales;     // 总销售额
    private Long totalUsers;           // 总用户数
    private Long totalBooks;           // 总图书数

    // 近7日趋势
    private List<TrendData> trendData; // 日期 → 销售额

    // 热门分类销量
    private List<CategorySalesData> categorySales;

    // 热门商品 Top10
    private List<HotBookData> hotBooks;

    @Data
    public static class TrendData {
        private String date;
        private BigDecimal amount;
    }

    @Data
    public static class CategorySalesData {
        private String name;
        private Long value;
    }

    @Data
    public static class HotBookData {
        private Long bookId;
        private String title;
        private String author;
        private Integer totalSales;
        private BigDecimal totalAmount;
    }
}