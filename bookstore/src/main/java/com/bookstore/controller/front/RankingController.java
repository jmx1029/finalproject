package com.bookstore.controller.front;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookstore.common.Result;
import com.bookstore.entity.Book;
import com.bookstore.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ranking")
public class RankingController {

    private final BookService bookService;

    // 构造器注入（推荐，避免字段注入警告）
    @Autowired
    public RankingController(BookService bookService) {
        this.bookService = bookService;
    }

    /**
     * 畅销榜：按销量降序 Top 50
     */
    @Cacheable(cacheNames = "ranking", key = "'sales'")
    @GetMapping("/sales")
    public Result<List<Book>> salesRanking() {
        List<Book> books = bookService.list(
                new LambdaQueryWrapper<Book>()
                        .eq(Book::getStatus, 1)
                        .eq(Book::getIsDeleted, 0)
                        .orderByDesc(Book::getSales)
                        .last("LIMIT 50")
        );
        return Result.success(books);
    }

    /**
     * 好评榜：按评分降序 Top 50（评分NULL的排除）
     */
    @Cacheable(cacheNames = "ranking", key = "'rating'")
    @GetMapping("/rating")
    public Result<List<Book>> ratingRanking() {
        List<Book> books = bookService.list(
                new LambdaQueryWrapper<Book>()
                        .eq(Book::getStatus, 1)
                        .eq(Book::getIsDeleted, 0)
                        .isNotNull(Book::getRating)
                        .orderByDesc(Book::getRating)
                        .last("LIMIT 50")
        );
        return Result.success(books);
    }

    /**
     * 推荐榜：综合评分（销量×0.6 + 评分×0.4）Top 50
     */
    @Cacheable(cacheNames = "ranking", key = "'recommend'")
    @GetMapping("/recommend")
    public Result<List<Book>> recommendRanking() {
        // 1. 查询所有上架图书
        List<Book> books = bookService.list(
                new LambdaQueryWrapper<Book>()
                        .eq(Book::getStatus, 1)
                        .eq(Book::getIsDeleted, 0)
        );

        if (books.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        // 2. 计算最大销量和最大评分（用于归一化）
        // 销量用 int 转 double
        double maxSales = books.stream()
                .mapToInt(Book::getSales)          // int 流
                .max()
                .orElse(1);

        // 评分用 BigDecimal 转 double
        double maxRating = books.stream()
                .filter(b -> b.getRating() != null)
                .mapToDouble(b -> b.getRating().doubleValue())  // ✅ 正确转换
                .max()
                .orElse(10.0);  // 10分制兜底

        // 3. 计算综合得分
        final double finalMaxSales = maxSales;
        final double finalMaxRating = maxRating;

        List<Map<String, Object>> ranked = books.stream()
                .map(book -> {
                    // 销量得分：销量 / 最大销量 × 100
                    double salesScore = finalMaxSales > 0
                            ? (double) book.getSales() / finalMaxSales * 100
                            : 0;

                    // 评分得分：评分 / 最大评分 × 100
                    double ratingScore = (book.getRating() != null && finalMaxRating > 0)
                            ? book.getRating().doubleValue() / finalMaxRating * 100  // ✅ 转换为 double
                            : 0;

                    // 综合得分 = 销量得分×0.6 + 评分得分×0.4
                    double compositeScore = salesScore * 0.6 + ratingScore * 0.4;

                    // 用 HashMap 替代 Map.of()，避免泛型推断导致的 Serializable 不匹配
                    Map<String, Object> item = new HashMap<>();
                    item.put("book", book);
                    item.put("score", compositeScore);
                    return item;
                })
                .sorted((a, b) -> Double.compare(
                        (Double) b.get("score"),
                        (Double) a.get("score")
                ))
                .limit(50)
                .collect(Collectors.toList());

        List<Book> result = ranked.stream()
                .map(m -> (Book) m.get("book"))
                .collect(Collectors.toList());

        return Result.success(result);
    }
}