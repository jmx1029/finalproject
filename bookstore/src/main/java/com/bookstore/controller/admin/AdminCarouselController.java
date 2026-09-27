package com.bookstore.controller.admin;

import com.bookstore.common.Result;
import com.bookstore.entity.Carousel;
import com.bookstore.service.CarouselService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admin/carousel")
public class AdminCarouselController {

    @Autowired
    private CarouselService carouselService;

    @GetMapping("/list")
    public Result<List<Carousel>> list() {
        return Result.success(carouselService.list());
    }

    @PostMapping
    public Result<Void> add(@RequestBody Carousel carousel) {
        carouselService.save(carousel);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody Carousel carousel) {
        carouselService.updateById(carousel);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        carouselService.removeById(id);
        return Result.success();
    }

    @PutMapping("/status/{id}")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        Carousel carousel = new Carousel();
        carousel.setId(id);
        carousel.setStatus(status);
        carouselService.updateById(carousel);
        return Result.success();
    }
}