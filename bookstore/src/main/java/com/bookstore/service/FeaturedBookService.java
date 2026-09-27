package com.bookstore.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bookstore.entity.Book;
import com.bookstore.entity.FeaturedBook;
import java.util.List;

public interface FeaturedBookService extends IService<FeaturedBook> {
    /**
     * 获取精选图书列表（含图书详情，按 sort 排序）
     */
    List<Book> getFeaturedBooksWithDetails();

    /**
     * 更新精选图书列表（先清空，再批量插入）
     */
    void updateFeaturedBooks(List<Long> bookIds);
}