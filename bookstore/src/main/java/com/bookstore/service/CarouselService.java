package com.bookstore.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bookstore.entity.Carousel;
import java.util.List;

public interface CarouselService extends IService<Carousel> {
    List<Carousel> getEnabledCarousels();
}