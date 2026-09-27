package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bookstore.entity.Category;
import com.bookstore.mapper.CategoryMapper;
import com.bookstore.service.CategoryService;
import com.bookstore.vo.CategoryVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    public List<CategoryVO> categoryTree() {
        // 1. 查所有分类
        List<Category> all = this.list(new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort));

        // 2. 转 VO
        List<CategoryVO> voList = all.stream().map(c -> {
            CategoryVO vo = new CategoryVO();
            BeanUtils.copyProperties(c, vo);
            vo.setChildren(new ArrayList<>());
            return vo;
        }).collect(Collectors.toList());

        // 3. 按 parentId 分组
        Map<Long, List<CategoryVO>> childrenMap = voList.stream()
                .collect(Collectors.groupingBy(CategoryVO::getParentId));

        // 4. 组装父子关系
        for (CategoryVO vo : voList) {
            vo.setChildren(childrenMap.getOrDefault(vo.getId(), new ArrayList<>()));
        }

        // 5. 返回顶级分类（parentId = 0）
        return childrenMap.getOrDefault(0L, new ArrayList<>());
    }
}