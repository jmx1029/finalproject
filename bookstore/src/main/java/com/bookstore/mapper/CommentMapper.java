package com.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bookstore.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Map;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    /**
     * 原子点赞：SQL 直接在数据库层完成 +1，防止并发丢更新
     */
    @Update("UPDATE tb_comment SET like_count = COALESCE(like_count, 0) + 1 WHERE id = #{id}")
    int incrementLikeCount(@Param("id") Long id);

    /**
     * 统计某本书的有效评论平均分和评论数（只统计顶级评论，回复无 rating）
     */
    @Select("SELECT ROUND(AVG(rating), 2) AS avg_rating, COUNT(*) AS cnt FROM tb_comment WHERE book_id = #{bookId} AND is_deleted = 0 AND parent_id IS NULL")
    Map<String, Object> getRatingAvgAndCount(@Param("bookId") Long bookId);

    /**
     * 判断用户是否购买过某本书（JOIN 一次搞定，避免 N+1）
     * 条件：订单项未删除 + 订单未删除 + 订单属于该用户 + 订单状态 >= 2（已发货/已完成）
     */
    @Select("SELECT 1 FROM tb_order_item oi " +
            "JOIN tb_order o ON oi.order_id = o.id " +
            "WHERE oi.book_id = #{bookId} " +
            "AND o.user_id = #{userId} " +
            "AND o.status >= 2 " +
            "AND oi.is_deleted = 0 " +
            "AND o.is_deleted = 0 " +
            "LIMIT 1")
    Integer checkPurchased(@Param("bookId") Long bookId, @Param("userId") Long userId);
}
