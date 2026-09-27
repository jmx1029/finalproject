package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bookstore.entity.Book;
import com.bookstore.entity.FeaturedBook;
import com.bookstore.mapper.FeaturedBookMapper;
import com.bookstore.service.BookService;
import com.bookstore.service.FeaturedBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class FeaturedBookServiceImpl extends ServiceImpl<FeaturedBookMapper, FeaturedBook> implements FeaturedBookService {

    @Autowired
    private BookService bookService;

    @Override
    public List<Book> getFeaturedBooksWithDetails() {
        // 1. 查询精选图书配置（按 sort 升序）
        LambdaQueryWrapper<FeaturedBook> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeaturedBook::getIsDeleted, 0)
                .orderByAsc(FeaturedBook::getSort);
        List<FeaturedBook> featuredBooks = this.list(wrapper);

        if (featuredBooks.isEmpty()) {
            // 如果配置为空，按销量取前8本作为兜底
            return bookService.list(
                    new LambdaQueryWrapper<Book>()
                            .eq(Book::getStatus, 1)
                            .eq(Book::getIsDeleted, 0)
                            .orderByDesc(Book::getSales)
                            .last("LIMIT 8")
            );
        }

        // 2. 根据 bookId 查询图书详情（保持顺序）
        List<Long> bookIds = featuredBooks.stream()
                .map(FeaturedBook::getBookId)
                .collect(java.util.stream.Collectors.toList());
        List<Book> books = bookService.listByIds(bookIds);

        // 3. 按照 featuredBooks 的顺序排序
        java.util.Map<Long, Book> bookMap = books.stream()
                .collect(java.util.stream.Collectors.toMap(Book::getId, b -> b));
        List<Book> result = new ArrayList<>();
        for (FeaturedBook fb : featuredBooks) {
            Book book = bookMap.get(fb.getBookId());
            if (book != null && book.getStatus() == 1 && book.getIsDeleted() == 0) {
                result.add(book);
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFeaturedBooks(List<Long> bookIds) {
        // 1. 清空所有精选配置
        this.remove(new LambdaQueryWrapper<FeaturedBook>());

        // 2. 批量插入新配置
        if (bookIds != null && !bookIds.isEmpty()) {
            List<FeaturedBook> list = new ArrayList<>();
            for (int i = 0; i < bookIds.size(); i++) {
                FeaturedBook fb = new FeaturedBook();
                fb.setBookId(bookIds.get(i));
                fb.setSort(i + 1);
                fb.setIsDeleted(0);
                list.add(fb);
            }
            this.saveBatch(list);
        }
    }
}