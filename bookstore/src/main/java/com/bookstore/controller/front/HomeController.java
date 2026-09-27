package com.bookstore.controller.front;

import com.bookstore.common.Result;
import com.bookstore.entity.Book;
import com.bookstore.entity.Carousel;
import com.bookstore.entity.Category;
import com.bookstore.service.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/home")
public class HomeController {

    @Autowired
    private CarouselService carouselService;

    @Autowired
    private BookService bookService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private HomeConfigService homeConfigService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FeaturedBookService featuredBookService;

    /**
     * 获取轮播图列表（含关联的书籍信息）
     */
    @Cacheable(cacheNames = "home", key = "'carousel'")
    @GetMapping("/carousel")
    public Result<List<Map<String, Object>>> getCarousel() {
        List<Carousel> carousels = carouselService.getEnabledCarousels();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Carousel carousel : carousels) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", carousel.getId());
            item.put("title", carousel.getTitle());
            item.put("description", carousel.getDescription());
            item.put("bgImageUrl", carousel.getBgImageUrl());
            item.put("sort", carousel.getSort());
            item.put("status", carousel.getStatus());

            // 解析 bookIds，查询对应的书籍信息
            String bookIdsStr = carousel.getBookIds();
            List<Book> books = new ArrayList<>();
            if (bookIdsStr != null && !bookIdsStr.isEmpty()) {
                List<Long> ids = Arrays.stream(bookIdsStr.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::parseLong)
                        .collect(Collectors.toList());
                if (!ids.isEmpty()) {
                    books = bookService.listByIds(ids);
                    // 保持与 bookIds 顺序一致
                    Map<Long, Book> bookMap = books.stream()
                            .collect(Collectors.toMap(Book::getId, b -> b));
                    books = ids.stream()
                            .map(bookMap::get)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                }
            }
            item.put("books", books);
            result.add(item);
        }

        return Result.success(result);
    }

    @Cacheable(cacheNames = "home", key = "'featuredBooks'")
    @GetMapping("/featured-books")
    public Result<List<Book>> getFeaturedBooks() {
        return Result.success(featuredBookService.getFeaturedBooksWithDetails());
    }

    @Cacheable(cacheNames = "home", key = "'featuredCategories'")
    @GetMapping("/featured-categories")
    public Result<List<Map<String, Object>>> getFeaturedCategories() {
        String configJson = homeConfigService.getFeaturedCategoriesConfig();
        try {
            List<Map<String, Object>> configList = objectMapper.readValue(
                    configJson,
                    new TypeReference<List<Map<String, Object>>>() {}
            );
            for (Map<String, Object> item : configList) {
                Object categoryIdObj = item.get("categoryId");
                if (categoryIdObj != null) {
                    Long categoryId = Long.valueOf(categoryIdObj.toString());
                    Category category = categoryService.getById(categoryId);
                    if (category != null) {
                        item.put("name", category.getName());
                        item.put("parentId", category.getParentId());
                    }
                }
            }
            return Result.success(configList);
        } catch (Exception e) {
            return Result.error("解析精选分类配置失败");
        }
    }
}