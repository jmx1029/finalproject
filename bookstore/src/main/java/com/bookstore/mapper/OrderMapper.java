package com.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bookstore.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    // ============================================================
    //  原有方法（大屏 Dashboard 依赖，必须保留）
    // ============================================================

    // 查询用户购买过的分类ID（推荐算法用）
    @Select("SELECT b.category_id FROM tb_order_item oi " +
            "JOIN tb_order o ON oi.order_id = o.id " +
            "JOIN tb_book b ON oi.book_id = b.id " +
            "WHERE o.user_id = #{userId} AND o.status >= 1 AND o.is_deleted = 0")
    List<Long> selectBoughtCategoryIds(@Param("userId") Long userId);


    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM tb_order WHERE shop_id = #{shopId} AND status >= 1 AND is_deleted = 0")
    BigDecimal getShopTotalSales(@Param("shopId") Long shopId);

    @Select("SELECT COUNT(*) FROM tb_order WHERE shop_id = #{shopId} AND is_deleted = 0")
    Long countShopOrders(@Param("shopId") Long shopId);

    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM tb_order WHERE shop_id = #{shopId} AND status >= 1 AND is_deleted = 0 AND DATE(create_time) = CURDATE()")
    BigDecimal getShopTodaySales(@Param("shopId") Long shopId);

    @Select("SELECT COUNT(*) FROM tb_order WHERE shop_id = #{shopId} AND is_deleted = 0 AND DATE(create_time) = CURDATE()")
    Long countShopTodayOrders(@Param("shopId") Long shopId);

    // 月度销售额趋势（大屏折线图）
    List<Map<String, Object>> getMonthlyTrend(@Param("year") Integer year);

    // 分类销量占比（大屏饼图）
    List<Map<String, Object>> getCategoryRatio();

    // 总销售额（大屏统计卡片）
    BigDecimal getTotalSales();


    // ============================================================
    //  新增：统计报表方法（第五阶段）
    // ============================================================

    // 总订单数
    @Select("SELECT COUNT(*) FROM tb_order WHERE is_deleted = 0")
    Long countTotalOrders();

    // 总销售额（与 getTotalSales 类似，但用于统计报表）
    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM tb_order WHERE status >= 1 AND is_deleted = 0")
    BigDecimal sumTotalSales();

    // 总用户数
    @Select("SELECT COUNT(DISTINCT user_id) FROM tb_order WHERE is_deleted = 0")
    Long countDistinctUsers();

    // 总图书数
    @Select("SELECT COUNT(*) FROM tb_book WHERE is_deleted = 0")
    Long countTotalBooks();

    // 近7日销售趋势（统计报表折线图）
    List<Map<String, Object>> getLast7DaysTrend();

    // 热门分类销量（统计报表饼图）
    List<Map<String, Object>> getCategorySales();

    // 热门商品 Top10（统计报表表格）
    List<Map<String, Object>> getHotBooksTop10();
}