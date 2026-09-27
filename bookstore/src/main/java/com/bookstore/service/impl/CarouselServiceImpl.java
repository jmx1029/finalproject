package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bookstore.entity.Carousel;
import com.bookstore.mapper.CarouselMapper;
import com.bookstore.service.CarouselService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CarouselServiceImpl extends ServiceImpl<CarouselMapper, Carousel> implements CarouselService {
    @Override
    public List<Carousel> getEnabledCarousels() {
        LambdaQueryWrapper<Carousel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Carousel::getStatus, 1)
                .eq(Carousel::getIsDeleted, 0)
                .orderByAsc(Carousel::getSort);
        return this.list(wrapper);
    }
}