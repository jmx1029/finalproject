package com.bookstore.controller.front;

import com.bookstore.common.Result;
import com.bookstore.entity.Book;
import com.bookstore.service.RecommenderService;
import com.bookstore.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommend")
public class RecommendController {

    @Autowired
    private RecommenderService recommenderService;

    /**
     * 获取推荐图书（基于用户购买历史）
     */
    @GetMapping
    public Result<List<Book>> recommend() {
        Long userId = UserContext.getUserId();
        return Result.success(recommenderService.recommendBooks(userId));
    }

    /**
     * 获取热门图书（冷启动用）
     */
    @GetMapping("/hot")
    public Result<List<Book>> hot() {
        return Result.success(recommenderService.getHotBooks(10));
    }
}