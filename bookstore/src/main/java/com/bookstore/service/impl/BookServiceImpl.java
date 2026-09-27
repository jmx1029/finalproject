package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bookstore.common.ErrorCode;
import com.bookstore.entity.Book;
import com.bookstore.exception.BusinessException;
import com.bookstore.mapper.BookMapper;
import com.bookstore.service.BookService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {

    @Override
    public IPage<Book> pageQuery(Integer page, Integer size, String keyword, Long categoryId, Integer status, String orderBy) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();

        // 关键词搜索
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Book::getTitle, keyword)
                    .or().like(Book::getAuthor, keyword)
                    .or().like(Book::getIsbn, keyword));
        }

        // 分类筛选
        if (categoryId != null) {
            wrapper.eq(Book::getCategoryId, categoryId);
        }

        // 状态筛选
        if (status != null) {
            wrapper.eq(Book::getStatus, status);
        }

        // 逻辑删除
        wrapper.eq(Book::getIsDeleted, 0);

        // ===== 排序逻辑 =====
        if ("sales".equals(orderBy)) {
            // 热门：按销量降序
            wrapper.orderByDesc(Book::getSales);
        } else if ("publish_date".equals(orderBy)) {
            // 最新：按出版日期降序
            wrapper.orderByDesc(Book::getPublishDate);
        } else if ("rating".equals(orderBy)) {
            // 好评：按评分降序，评分NULL的排在最后
            wrapper.orderByDesc(Book::getRating);
        } else {
            // 默认：按id降序（最新添加）
            wrapper.orderByDesc(Book::getId);
        }

        return this.page(new Page<>(page, size), wrapper);
    }

    @Override
    public List<Map<String, Object>> getSearchSuggestions(String keyword, Integer limit) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(Book::getId, Book::getTitle, Book::getAuthor, Book::getCoverUrl)
                .eq(Book::getStatus, 1)
                .eq(Book::getIsDeleted, 0)
                .and(w -> w.like(Book::getTitle, keyword)
                        .or().like(Book::getAuthor, keyword)
                        .or().like(Book::getIsbn, keyword))
                .orderByDesc(Book::getSales)
                .last("LIMIT " + limit);
        List<Book> books = this.list(wrapper);
        return books.stream().map(book -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", book.getId());
            item.put("title", book.getTitle());
            item.put("author", book.getAuthor());
            item.put("coverUrl", book.getCoverUrl());
            return item;
        }).collect(Collectors.toList());
    }

    // ===== 新增：商家专用方法 =====
    @Override
    public IPage<Book> pageQueryByShop(Long shopId, Integer page, Integer size, String keyword, Long categoryId, Integer status, String orderBy) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();

        // 只查当前店铺的图书
        wrapper.eq(Book::getShopId, shopId)
                .eq(Book::getIsDeleted, 0);

        // 关键词搜索
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Book::getTitle, keyword)
                    .or().like(Book::getAuthor, keyword)
                    .or().like(Book::getIsbn, keyword));
        }

        // 分类筛选
        if (categoryId != null) {
            wrapper.eq(Book::getCategoryId, categoryId);
        }

        // 状态筛选
        if (status != null) {
            wrapper.eq(Book::getStatus, status);
        }

        // 排序
        if ("sales".equals(orderBy)) {
            wrapper.orderByDesc(Book::getSales);
        } else if ("publish_date".equals(orderBy)) {
            wrapper.orderByDesc(Book::getPublishDate);
        } else if ("rating".equals(orderBy)) {
            wrapper.orderByDesc(Book::getRating);
        } else {
            wrapper.orderByDesc(Book::getCreateTime);
        }

        return this.page(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addBookByShop(Long shopId, Book book) {
        book.setShopId(shopId);
        book.setIsAdminCreated(0);
        book.setIsDeleted(0);
        book.setCreateTime(LocalDateTime.now());
        book.setUpdateTime(LocalDateTime.now());
        this.save(book);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBookByShop(Long shopId, Book book) {
        // 校验该图书是否属于当前店铺
        Book exist = this.getById(book.getId());
        if (exist == null) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }
        if (exist.getShopId() == null || !exist.getShopId().equals(shopId)) {
            throw new BusinessException(ErrorCode.BOOK_NO_PERMISSION_MODIFY);
        }
        // 保护敏感字段：从数据库原值复制，防止被 Mass Assignment 篡改
        book.setShopId(exist.getShopId());
        book.setIsAdminCreated(exist.getIsAdminCreated());
        book.setIsDeleted(exist.getIsDeleted());
        book.setSales(exist.getSales());
        book.setRating(exist.getRating());
        book.setUpdateTime(LocalDateTime.now());
        this.updateById(book);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBookByShop(Long shopId, Long bookId) {
        Book book = this.getById(bookId);
        if (book == null) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }
        if (!book.getShopId().equals(shopId)) {
            throw new BusinessException(ErrorCode.BOOK_NO_PERMISSION_DELETE);
        }
        book.setIsDeleted(1);
        this.updateById(book);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatusByShop(Long shopId, Long bookId, Integer status) {
        Book book = this.getById(bookId);
        if (book == null) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }
        if (!book.getShopId().equals(shopId)) {
            throw new BusinessException(ErrorCode.BOOK_NO_PERMISSION_OPERATE);
        }
        book.setStatus(status);
        book.setUpdateTime(LocalDateTime.now());
        this.updateById(book);
    }

    // ===== 相关推荐 =====
    @Override
    public List<Book> getRecommend(Long categoryId, Long excludeBookId, Integer limit) {
        if (limit == null || limit <= 0) limit = 8;
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Book::getStatus, 1)
                .eq(Book::getIsDeleted, 0);

        // 排除自己
        if (excludeBookId != null) {
            wrapper.ne(Book::getId, excludeBookId);
        }

        // 同分类优先
        if (categoryId != null) {
            wrapper.eq(Book::getCategoryId, categoryId);
        }

        // 按销量+评分排序，取前 N 本
        wrapper.orderByDesc(Book::getSales)
                .orderByDesc(Book::getRating)
                .last("LIMIT " + limit);

        List<Book> result = this.list(wrapper);

        // 如果同分类不够，再补充几本不限分类的热门
        if (result.size() < limit && excludeBookId != null) {
            int need = limit - result.size();
            // 先收集已选书籍 ID，在 SQL 层排除，避免 LIMIT 后 Java 去重导致数量不足
            Set<Long> existingIds = result.stream().map(Book::getId).collect(Collectors.toSet());
            LambdaQueryWrapper<Book> fallback = new LambdaQueryWrapper<>();
            fallback.eq(Book::getStatus, 1)
                    .eq(Book::getIsDeleted, 0)
                    .ne(Book::getId, excludeBookId);
            if (!existingIds.isEmpty()) {
                fallback.notIn(Book::getId, existingIds);
            }
            fallback.orderByDesc(Book::getSales)
                    .orderByDesc(Book::getRating)
                    .last("LIMIT " + need);
            List<Book> extra = this.list(fallback);
            result.addAll(extra);
        }

        return result;
    }

}