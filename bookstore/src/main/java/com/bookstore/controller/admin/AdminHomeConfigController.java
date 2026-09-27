package com.bookstore.controller.admin;

import com.bookstore.common.Result;
import com.bookstore.service.HomeConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/home-config")
public class AdminHomeConfigController {

    @Autowired
    private HomeConfigService homeConfigService;

    @GetMapping("/featured-categories")
    public Result<String> getFeaturedCategories() {
        return Result.success(homeConfigService.getFeaturedCategoriesConfig());
    }

    @PutMapping("/featured-categories")
    public Result<Void> updateFeaturedCategories(@RequestBody Map<String, String> body) {
        String configValue = body.get("configValue");
        homeConfigService.updateFeaturedCategories(configValue);
        return Result.success();
    }
}