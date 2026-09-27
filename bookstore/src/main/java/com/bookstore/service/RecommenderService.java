package com.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookstore.entity.Book;
import com.bookstore.mapper.BookMapper;
import com.bookstore.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RecommenderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String RECOMMEND_KEY_PREFIX = "recommend:user:";

    /**
     * 为用户推荐图书（基于购买历史分类）
     * 1. 有购买历史 → 统计购买分类 → 取 Top2 → 推荐该分类下销量最高且未购买的图书
     * 2. 无购买历史 → 冷启动 → 返回热门图书 Top10
     * 3. 推荐结果不足 → 用热门图书补齐到 10 本
     * 4. 结果存入 Redis 缓存 24 小时
     */
    @SuppressWarnings("unchecked")
    public List<Book> recommendBooks(Long userId) {
        String key = RECOMMEND_KEY_PREFIX + userId;

        // 1. 先查 Redis 缓存
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            log.info("推荐结果命中缓存，用户：{}", userId);
            return (List<Book>) cached;
        }

        // 2. 查询用户购买过的分类ID
        List<Long> categoryIds = orderMapper.selectBoughtCategoryIds(userId);
        List<Book> result;

        if (categoryIds == null || categoryIds.isEmpty()) {
            // 新用户冷启动：返回热门图书
            log.info("新用户冷启动，返回热门图书，用户：{}", userId);
            result = getHotBooks(10);
        } else {
            // 3. 统计分类频率，取 Top2
            Map<Long, Long> countMap = categoryIds.stream()
                    .collect(Collectors.groupingBy(c -> c, Collectors.counting()));
            List<Long> topCategories = countMap.entrySet().stream()
                    .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                    .limit(2)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            log.info("用户购买分类统计：{}，取Top2：{}", countMap, topCategories);

            // 4. 查询这些分类下销量最高且用户未购买的 Top 10
            result = bookMapper.selectRecommendByCategory(topCategories, userId, 10);
            log.info("基于分类推荐结果：{} 本", result.size());

            // 5. 推荐结果不足，用热门图书补齐
            if (result.size() < 10) {
                List<Book> hotBooks = getHotBooks(10 - result.size());
                Set<Long> existingIds = result.stream().map(Book::getId).collect(Collectors.toSet());
                for (Book book : hotBooks) {
                    if (!existingIds.contains(book.getId())) {
                        result.add(book);
                    }
                }
                log.info("用热门图书补齐，当前共：{} 本", result.size());
            }
        }

        // 6. 存入 Redis，缓存24小时
        redisTemplate.opsForValue().set(key, result, 24, TimeUnit.HOURS);
        log.info("推荐结果计算完成并缓存，用户：{}，数量：{}", userId, result.size());
        return result;
    }

    /**
     * 热门图书（按销量排序）
     */
    public List<Book> getHotBooks(int limit) {
        return bookMapper.selectList(
                new LambdaQueryWrapper<Book>()
                        .eq(Book::getStatus, 1)
                        .eq(Book::getIsDeleted, 0)   // 只查有效图书
                        .orderByDesc(Book::getSales)
                        .last("LIMIT " + limit)
        );
    }

    /**
     * 清除用户推荐缓存（下单后调用）
     */
    public void clearRecommendCache(Long userId) {
        String key = RECOMMEND_KEY_PREFIX + userId;
        Boolean deleted = redisTemplate.delete(key);
        log.info("清除推荐缓存，用户：{}，{}", userId, Boolean.TRUE.equals(deleted) ? "成功" : "失败（缓存不存在）");
    }
}