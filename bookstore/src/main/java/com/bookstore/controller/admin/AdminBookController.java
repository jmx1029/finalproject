package com.bookstore.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bookstore.common.ErrorCode;
import com.bookstore.common.Result;
import com.bookstore.dto.BookCreateDTO;
import com.bookstore.dto.BookUpdateDTO;
import com.bookstore.entity.Book;
import com.bookstore.exception.BusinessException;
import com.bookstore.service.BookService;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/admin/book")
public class AdminBookController {

    @Autowired
    private BookService bookService;

    /**
     * 管理员查看所有图书（包含所有商家的）
     */
    @GetMapping("/page")
    public Result<IPage<Book>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status) {
        return Result.success(bookService.pageQuery(page, size, keyword, categoryId, status, "id"));
    }

    /**
     * 管理员新增图书
     * 权限：管理员不受 shopId 归属限制，创建的图书标记为官方自营
     */
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> add(@Valid @RequestBody BookCreateDTO dto) {
        Book book = new Book();
        BeanUtils.copyProperties(dto, book);
        // 管理员创建 → 官方自营，敏感字段手动设置
        book.setShopId(null);           // 官方自营无 shopId
        book.setIsAdminCreated(1);      // 标记管理员创建
        book.setStatus(1);              // 默认上架
        book.setIsDeleted(0);
        book.setSales(0);
        book.setCreateTime(LocalDateTime.now());
        book.setUpdateTime(LocalDateTime.now());
        bookService.save(book);
        return Result.success();
    }

    /**
     * 管理员编辑图书
     * 权限：管理员可编辑任意店铺的商品，不受 shopId 归属限制
     * 保护：sales/rating/isDeleted/shopId/isAdminCreated 从数据库原值复制，防止 Mass Assignment
     */
    @PutMapping
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> update(@Valid @RequestBody BookUpdateDTO dto) {
        Book exist = bookService.getById(dto.getId());
        if (exist == null) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }
        Book book = new Book();
        BeanUtils.copyProperties(dto, book);
        // 保护敏感字段：从数据库原值复制，管理员也不能通过编辑篡改销量/评分/归属
        book.setShopId(exist.getShopId());
        book.setIsAdminCreated(exist.getIsAdminCreated());
        book.setIsDeleted(exist.getIsDeleted());
        book.setSales(exist.getSales());
        book.setRating(exist.getRating());
        book.setStatus(exist.getStatus()); // status 仍走独立接口 /status 管理，编辑不覆盖
        book.setUpdateTime(LocalDateTime.now());
        bookService.updateById(book);
        return Result.success();
    }

    /**
     * 管理员更新图书状态（下架/上架）
     * 使用 /status 接口，接收 id 和 status 参数
     */
    @PutMapping("/status")
    public Result<Void> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        Book book = bookService.getById(id);
        if (book == null) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }
        book.setStatus(status);
        bookService.updateById(book);
        return Result.success();
    }

    /**
     * 管理员删除图书（逻辑删除，慎重）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Book book = bookService.getById(id);
        if (book == null) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }
        book.setIsDeleted(1);
        bookService.updateById(book);
        return Result.success();
    }
}
