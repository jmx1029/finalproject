package com.bookstore.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bookstore.entity.Category;
import com.bookstore.vo.CategoryVO;
import java.util.List;

public interface CategoryService extends IService<Category> {
    List<CategoryVO> categoryTree();
}