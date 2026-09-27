package com.bookstore.controller.front;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookstore.common.ErrorCode;
import com.bookstore.common.Result;
import com.bookstore.entity.Book;
import com.bookstore.exception.BusinessException;
import com.bookstore.entity.Shop;
import com.bookstore.mapper.CommentMapper;
import com.bookstore.service.BookService;
import com.bookstore.service.ShopService;
import com.bookstore.vo.BookDetailVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/books")
public class BookController {

    @Autowired
    private BookService bookService;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private ShopService shopService;

    // 前台分页
    @GetMapping("/page")
    public Result<IPage<Book>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long cid,
            @RequestParam(defaultValue = "sales") String orderBy) {
        return Result.success(bookService.pageQuery(page, size, keyword, cid, 1, orderBy));
    }

    // ===== 增强的图书详情接口 =====
    @GetMapping("/{id}")
    public Result<BookDetailVO> detail(@PathVariable Long id) {
        Book book = bookService.getById(id);
        if (book == null) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }

        // 转换为 VO
        BookDetailVO vo = new BookDetailVO();
        BeanUtils.copyProperties(book, vo);

        // ===== 查询店铺名称 =====
        if (book.getShopId() != null) {
            Shop shop = shopService.getById(book.getShopId());
            vo.setShopName(shop != null ? shop.getName() : "未知店铺");
        } else {
            vo.setShopName("官方自营");
        }

        // 计算平均评分和评分人数（复用 CommentMapper，保证与 recalculateBookRating 逻辑一致）
        Map<String, Object> stats = commentMapper.getRatingAvgAndCount(id);
        if (stats != null) {
            Long cnt = ((Number) stats.get("cnt")).longValue();
            if (cnt > 0) {
                Object avgObj = stats.get("avg_rating");
                if (avgObj != null) {
                    double avg = ((Number) avgObj).doubleValue();
                    vo.setAvgRating(Math.round(avg * 10) / 10.0);
                }
                vo.setRatingCount(cnt.intValue());
            }
        }

        return Result.success(vo);
    }

    /**
     * 搜索建议（输入联想）
     */
    @GetMapping("/suggest")
    public Result<List<Map<String, Object>>> suggest(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "8") Integer limit) {
        List<Map<String, Object>> result = bookService.getSearchSuggestions(keyword, limit);
        return Result.success(result);
    }

    /**
     * 相关推荐（同分类热门图书，用于商品详情页"猜你喜欢"）
     * @param excludeBookId 排除当前这本书（可选，不传就不限）
     * @param categoryId 同分类优先（可选）
     * @param limit 推荐数量，默认 8
     */
    @Cacheable(cacheNames = "book", key = "'recommend:' + #categoryId + ':' + #excludeBookId + ':' + #limit")
    @GetMapping("/recommend")
    public Result<List<Book>> recommend(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long excludeBookId,
            @RequestParam(defaultValue = "8") Integer limit) {
        return Result.success(bookService.getRecommend(categoryId, excludeBookId, limit));
    }

}