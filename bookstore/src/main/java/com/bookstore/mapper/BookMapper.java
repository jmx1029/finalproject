package com.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bookstore.entity.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface BookMapper extends BaseMapper<Book> {

    // ===== 原有方法（注解方式，保持不变） =====

    @Select("SELECT COUNT(*) FROM tb_book WHERE shop_id = #{shopId} AND stock <= 10 AND status = 1 AND is_deleted = 0")
    Integer getShopLowStockCount(@Param("shopId") Long shopId);

    @Select("SELECT COUNT(*) FROM tb_book WHERE shop_id = #{shopId} AND is_deleted = 0")
    Long countShopBooks(@Param("shopId") Long shopId);

    @Select("SELECT id, title, author, cover_url, price, sales, stock FROM tb_book WHERE shop_id = #{shopId} AND status = 1 AND is_deleted = 0 ORDER BY sales DESC LIMIT #{limit}")
    List<Map<String, Object>> getShopHotBooks(@Param("shopId") Long shopId, @Param("limit") int limit);

    @Select("SELECT id, title, author, cover_url, price, stock FROM tb_book WHERE shop_id = #{shopId} AND stock <= 10 AND status = 1 AND is_deleted = 0 ORDER BY stock ASC")
    List<Map<String, Object>> getShopStockWarning(@Param("shopId") Long shopId);

    @Update("UPDATE tb_book SET stock = stock - #{num}, sales = sales + #{num} WHERE id = #{id} AND stock >= #{num} AND is_deleted = 0")
    int decreaseStock(@Param("id") Long id, @Param("num") Integer num);

    /** 原子恢复库存（售后退款时用） */
    @Update("UPDATE tb_book SET stock = stock + #{num} WHERE id = #{id} AND is_deleted = 0")
    int increaseStock(@Param("id") Long id, @Param("num") Integer num);

    /** 原子扣减销量（售后退款时用，与下单时的 sales+sale 对称） */
    @Update("UPDATE tb_book SET sales = sales - #{num} WHERE id = #{id} AND is_deleted = 0 AND sales >= #{num}")
    int decreaseSales(@Param("id") Long id, @Param("num") Integer num);

    /**
     * 回写图书评分（评论新增/删除时调用，直接覆盖爬虫初始分）
     */
    @Update("UPDATE tb_book SET rating = #{rating} WHERE id = #{bookId}")
    int updateRating(@Param("bookId") Long bookId,
                     @Param("rating") java.math.BigDecimal rating);

    @Select("SELECT title, stock FROM tb_book WHERE stock < 10 AND is_deleted = 0 ORDER BY stock ASC LIMIT 10")
    List<Map<String, Object>> getStockWarning();

    @Select("SELECT COUNT(*) FROM tb_book WHERE stock < 10 AND is_deleted = 0")
    Integer getLowStockCount();

    // ===== 删除 @Select 注解，只保留方法签名 =====
    // 这个方法现在通过 BookMapper.xml 实现
    List<Book> selectRecommendByCategory(@Param("categoryIds") List<Long> categoryIds,
                                         @Param("userId") Long userId,
                                         @Param("limit") int limit);
}