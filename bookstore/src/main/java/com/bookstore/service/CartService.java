package com.bookstore.service;

import com.bookstore.vo.CartItemVO;
import java.util.List;

public interface CartService {
    void addToCart(Long userId, Long bookId, Integer quantity);
    void updateQuantity(Long userId, Long bookId, Integer quantity);
    void removeFromCart(Long userId, Long bookId);
    void toggleSelect(Long userId, Long bookId, Boolean selected);
    void toggleSelectAll(Long userId, Boolean selected);
    List<CartItemVO> getCart(Long userId);
    void clearCart(Long userId);
    List<CartItemVO> getSelectedItems(Long userId);
    /** 批量删除（按 bookId 列表） */
    void deleteBatch(Long userId, List<Long> bookIds);
}