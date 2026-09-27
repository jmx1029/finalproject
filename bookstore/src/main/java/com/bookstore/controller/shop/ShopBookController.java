package com.bookstore.controller.shop;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bookstore.common.ErrorCode;
import com.bookstore.common.Result;
import com.bookstore.dto.BookCreateDTO;
import com.bookstore.dto.BookUpdateDTO;
import com.bookstore.entity.Book;
import com.bookstore.exception.BusinessException;
import com.bookstore.service.BookService;
import com.bookstore.service.ShopService;
import com.bookstore.util.UserContext;
import com.bookstore.vo.ShopVO;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shop/book")
public class ShopBookController {

    @Autowired
    private BookService bookService;

    @Autowired
    private ShopService shopService;

    private Long getCurrentShopId() {
        ShopVO shop = shopService.getShopByUserId(UserContext.getUserId());
        if (shop == null) {
            throw new BusinessException(ErrorCode.NOT_SHOP_OWNER);
        }
        return shop.getId();
    }

    @GetMapping("/page")
    public Result<IPage<Book>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "sales") String orderBy) {
        Long shopId = getCurrentShopId();
        return Result.success(bookService.pageQueryByShop(shopId, page, size, keyword, categoryId, status, orderBy));
    }

    /**
     * 商家新建图书
     * 只接收 BookCreateDTO（业务字段），敏感字段在 Service 层设置
     */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody BookCreateDTO dto) {
        Long shopId = getCurrentShopId();
        Book book = new Book();
        BeanUtils.copyProperties(dto, book);
        bookService.addBookByShop(shopId, book);
        return Result.success();
    }

    /**
     * 商家修改图书
     * 只接收 BookUpdateDTO（业务字段），敏感字段保留原值不被覆盖
     */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody BookUpdateDTO dto) {
        Long shopId = getCurrentShopId();
        Book book = new Book();
        BeanUtils.copyProperties(dto, book);
        bookService.updateBookByShop(shopId, book);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long shopId = getCurrentShopId();
        bookService.deleteBookByShop(shopId, id);
        return Result.success();
    }

    @PutMapping("/status")
    public Result<Void> toggleStatus(@RequestParam Long id, @RequestParam Integer status) {
        Long shopId = getCurrentShopId();
        bookService.toggleStatusByShop(shopId, id, status);
        return Result.success();
    }
}
