package com.bookstore.service.impl;

import com.bookstore.common.ErrorCode;
import com.bookstore.entity.Book;
import com.bookstore.exception.BusinessException;
import com.bookstore.service.BookService;
import com.bookstore.service.CartService;
import com.bookstore.vo.CartItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private BookService bookService;

    private String getCartKey(Long userId) {
        return "cart:" + userId;
    }

    @Override
    public void addToCart(Long userId, Long bookId, Integer quantity) {
        // 1. 先查询图书库存
        Book book = bookService.getById(bookId);
        if (book == null) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }

        String key = getCartKey(userId);
        String hashKey = bookId.toString();

        // 2. 从 Redis 中获取购物车项
        CartItemVO item = (CartItemVO) redisTemplate.opsForHash().get(key, hashKey);

        // 3. 计算加购后的总数量
        int newQuantity = quantity;
        if (item != null) {
            newQuantity = item.getQuantity() + quantity;
        }

        // 4. 加购时校验库存
        if (book.getStock() < newQuantity) {
            throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT,
                    "库存不足，当前库存仅剩 " + book.getStock() + " 件");
        }

        // 5. 构建或更新购物车项
        if (item == null) {
            item = new CartItemVO();
            item.setBookId(bookId);
            item.setTitle(book.getTitle());
            item.setCoverUrl(book.getCoverUrl());
            item.setPrice(book.getPrice());
            item.setStock(book.getStock());
            item.setQuantity(quantity);
            item.setSelected(true);
        } else {
            // ===== 关键修复：更新数量，而不是新增 =====
            item.setQuantity(newQuantity);
            // 更新库存信息（防止管理员修改了库存）
            item.setStock(book.getStock());
            // 更新价格（防止价格变动）
            item.setPrice(book.getPrice());
        }

        // 6. 存入 Redis（使用 put，不是 putAll，确保覆盖）
        redisTemplate.opsForHash().put(key, hashKey, item);
    }

    @Override
    public void updateQuantity(Long userId, Long bookId, Integer quantity) {
        String key = getCartKey(userId);
        String hashKey = bookId.toString();
        CartItemVO item = (CartItemVO) redisTemplate.opsForHash().get(key, hashKey);

        if (item == null) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        // ===== 校验库存 =====
        Book book = bookService.getById(bookId);
        if (book != null && book.getStock() < quantity) {
            throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT,
                    "库存不足，当前库存仅剩 " + book.getStock() + " 件");
        }

        item.setQuantity(quantity);
        if (book != null) {
            item.setStock(book.getStock());
        }
        redisTemplate.opsForHash().put(key, hashKey, item);
    }

    @Override
    public void removeFromCart(Long userId, Long bookId) {
        redisTemplate.opsForHash().delete(getCartKey(userId), bookId.toString());
    }

    @Override
    public void deleteBatch(Long userId, List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) return;
        Object[] fields = bookIds.stream().map(String::valueOf).toArray();
        redisTemplate.opsForHash().delete(getCartKey(userId), fields);
    }

    @Override
    public void toggleSelect(Long userId, Long bookId, Boolean selected) {
        String key = getCartKey(userId);
        CartItemVO item = (CartItemVO) redisTemplate.opsForHash().get(key, bookId.toString());
        if (item != null) {
            item.setSelected(selected);
            redisTemplate.opsForHash().put(key, bookId.toString(), item);
        }
    }

    @Override
    public void toggleSelectAll(Long userId, Boolean selected) {
        String key = getCartKey(userId);
        List<Object> values = redisTemplate.opsForHash().values(key);
        for (Object obj : values) {
            CartItemVO item = (CartItemVO) obj;
            item.setSelected(selected);
            redisTemplate.opsForHash().put(key, item.getBookId().toString(), item);
        }
    }

    @Override
    public List<CartItemVO> getCart(Long userId) {
        List<Object> values = redisTemplate.opsForHash().values(getCartKey(userId));
        List<CartItemVO> result = new ArrayList<>();
        for (Object obj : values) {
            result.add((CartItemVO) obj);
        }
        return result;
    }

    @Override
    public void clearCart(Long userId) {
        redisTemplate.delete(getCartKey(userId));
    }

    @Override
    public List<CartItemVO> getSelectedItems(Long userId) {
        List<CartItemVO> all = getCart(userId);
        List<CartItemVO> selected = new ArrayList<>();
        for (CartItemVO item : all) {
            if (Boolean.TRUE.equals(item.getSelected())) {
                selected.add(item);
            }
        }
        return selected;
    }
}