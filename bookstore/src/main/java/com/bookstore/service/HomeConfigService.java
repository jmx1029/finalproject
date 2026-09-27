package com.bookstore.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bookstore.entity.HomeConfig;

public interface HomeConfigService extends IService<HomeConfig> {
    String getFeaturedCategoriesConfig();
    void updateFeaturedCategories(String configValue);
}