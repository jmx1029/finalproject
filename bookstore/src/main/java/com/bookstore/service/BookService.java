package com.bookstore.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bookstore.entity.Book;

import java.util.List;
import java.util.Map;

public interface BookService extends IService<Book> {

    // ===== 原有方法 =====
    IPage<Book> pageQuery(Integer page, Integer size, String keyword, Long categoryId, Integer status, String orderBy);
    List<Map<String, Object>> getSearchSuggestions(String keyword, Integer limit);

    // ===== 新增：商家专用方法 =====
    /**
     * 商家分页查询自己的商品
     */
    IPage<Book> pageQueryByShop(Long shopId, Integer page, Integer size, String keyword, Long categoryId, Integer status, String orderBy);

    /**
     * 商家新增商品
     */
    void addBookByShop(Long shopId, Book book);

    /**
     * 商家更新商品
     */
    void updateBookByShop(Long shopId, Book book);

    /**
     * 商家删除商品（逻辑删除）
     */
    void deleteBookByShop(Long shopId, Long bookId);

    /**
     * 商家上下架商品
     */
    void toggleStatusByShop(Long shopId, Long bookId, Integer status);

    // ===== 相关推荐 =====
    /**
     * 获取同分类相关图书（排除自己，按销量+评分排序）
     * @param categoryId 分类ID
     * @param excludeBookId 排除的图书ID
     * @param limit 取多少本
     */
    List<Book> getRecommend(Long categoryId, Long excludeBookId, Integer limit);
}