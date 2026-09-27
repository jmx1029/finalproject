package com.bookstore.controller.front;

import com.bookstore.common.Result;
import com.bookstore.service.CartService;
import com.bookstore.util.UserContext;
import com.bookstore.vo.CartItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping
    public Result<List<CartItemVO>> getCart() {
        return Result.success(cartService.getCart(UserContext.getUserId()));
    }

    @PostMapping("/add")
    public Result<Void> addToCart(@RequestBody Map<String, Object> params) {
        Long bookId = Long.valueOf(params.get("bookId").toString());
        Integer quantity = Integer.valueOf(params.get("quantity").toString());
        cartService.addToCart(UserContext.getUserId(), bookId, quantity);
        return Result.success();
    }

    @PutMapping("/update")
    public Result<Void> updateQuantity(@RequestBody Map<String, Object> params) {
        Long bookId = Long.valueOf(params.get("bookId").toString());
        Integer quantity = Integer.valueOf(params.get("quantity").toString());
        cartService.updateQuantity(UserContext.getUserId(), bookId, quantity);
        return Result.success();
    }

    @DeleteMapping("/{bookId}")
    public Result<Void> remove(@PathVariable Long bookId) {
        cartService.removeFromCart(UserContext.getUserId(), bookId);
        return Result.success();
    }

    /** 批量删除（按选中的 bookId 列表） */
    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Integer> idsInt = (List<Integer>) params.get("bookIds");
        if (idsInt != null && !idsInt.isEmpty()) {
            List<Long> bookIds = idsInt.stream().map(Long::valueOf).toList();
            cartService.deleteBatch(UserContext.getUserId(), bookIds);
        }
        return Result.success();
    }

    /** 清空购物车 */
    @DeleteMapping("/clear")
    public Result<Void> clear() {
        cartService.clearCart(UserContext.getUserId());
        return Result.success();
    }

    @PutMapping("/select")
    public Result<Void> toggleSelect(@RequestBody Map<String, Object> params) {
        Long bookId = Long.valueOf(params.get("bookId").toString());
        Boolean selected = Boolean.valueOf(params.get("selected").toString());
        cartService.toggleSelect(UserContext.getUserId(), bookId, selected);
        return Result.success();
    }

    @PutMapping("/selectAll")
    public Result<Void> selectAll(@RequestBody Map<String, Object> params) {
        Boolean selected = Boolean.valueOf(params.get("selected").toString());
        cartService.toggleSelectAll(UserContext.getUserId(), selected);
        return Result.success();
    }
}