package com.bookstore.controller.admin;

import com.bookstore.common.Result;
import com.bookstore.entity.Book;
import com.bookstore.service.BookService;
import com.bookstore.service.FeaturedBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/featured-books")
public class AdminFeaturedBookController {

    @Autowired
    private FeaturedBookService featuredBookService;

    @Autowired
    private BookService bookService;

    /**
     * 获取当前精选图书列表（含图书详情）
     */
    @GetMapping
    public Result<List<Book>> getFeaturedBooks() {
        return Result.success(featuredBookService.getFeaturedBooksWithDetails());
    }

    /**
     * 更新精选图书列表
     */
    @PutMapping
    public Result<Void> updateFeaturedBooks(@RequestBody Map<String, List<Long>> body) {
        List<Long> bookIds = body.get("bookIds");
        featuredBookService.updateFeaturedBooks(bookIds);
        return Result.success();
    }

    /**
     * 获取所有可选的图书（供选择列表使用）
     */
    @GetMapping("/available")
    public Result<List<Book>> getAvailableBooks() {
        List<Book> books = bookService.list(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Book>()
                        .eq(Book::getStatus, 1)
                        .eq(Book::getIsDeleted, 0)
                        .orderByDesc(Book::getSales)
        );
        return Result.success(books);
    }
}