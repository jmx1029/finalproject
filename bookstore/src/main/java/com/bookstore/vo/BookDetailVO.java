package com.bookstore.vo;

import com.bookstore.entity.Book;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BookDetailVO extends Book {
    private Double avgRating;      // 平均评分
    private Integer ratingCount;   // 评分人数
    private String shopName;
}