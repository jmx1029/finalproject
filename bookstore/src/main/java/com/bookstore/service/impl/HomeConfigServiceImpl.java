package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bookstore.entity.HomeConfig;
import com.bookstore.mapper.HomeConfigMapper;
import com.bookstore.service.HomeConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomeConfigServiceImpl extends ServiceImpl<HomeConfigMapper, HomeConfig> implements HomeConfigService {
    private static final String CONFIG_KEY = "featured_categories";

    @Override
    public String getFeaturedCategoriesConfig() {
        HomeConfig config = this.getOne(
                new LambdaQueryWrapper<HomeConfig>().eq(HomeConfig::getConfigKey, CONFIG_KEY)
        );
        return config != null ? config.getConfigValue() : "[]";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFeaturedCategories(String configValue) {
        HomeConfig config = this.getOne(
                new LambdaQueryWrapper<HomeConfig>().eq(HomeConfig::getConfigKey, CONFIG_KEY)
        );
        if (config == null) {
            config = new HomeConfig();
            config.setConfigKey(CONFIG_KEY);
        }
        config.setConfigValue(configValue);
        this.saveOrUpdate(config);
    }
}
