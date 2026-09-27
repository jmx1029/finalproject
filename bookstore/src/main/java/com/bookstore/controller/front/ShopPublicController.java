package com.bookstore.controller.front;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bookstore.common.ErrorCode;
import com.bookstore.common.Result;
import com.bookstore.entity.Book;
import com.bookstore.exception.BusinessException;
import com.bookstore.entity.Shop;
import com.bookstore.service.BookService;
import com.bookstore.service.ShopService;
import com.bookstore.vo.ShopVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop/public")
public class ShopPublicController {

    @Autowired
    private ShopService shopService;

    @Autowired
    private BookService bookService;

    /**
     * 获取店铺公开信息（无需登录）
     */
    @Cacheable(cacheNames = "shop", key = "#shopId")
    @GetMapping("/{shopId}")
    public Result<ShopVO> getShopInfo(@PathVariable Long shopId) {
        Shop shop = shopService.getById(shopId);
        if (shop == null) {
            throw new BusinessException(ErrorCode.SHOP_NOT_OWNED);
        }
        ShopVO vo = new ShopVO();
        BeanUtils.copyProperties(shop, vo);
        return Result.success(vo);
    }

    /**
     * 获取店铺的商品列表（分页，仅上架商品）
     */
    @GetMapping("/{shopId}/books")
    public Result<IPage<Book>> getShopBooks(
            @PathVariable Long shopId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        // 只返回上架的商品
        return Result.success(bookService.pageQueryByShop(shopId, page, size, keyword, null, 1, "sales"));
    }
}