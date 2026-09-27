package com.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bookstore.entity.Shop;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ShopMapper extends BaseMapper<Shop> {
    @Select("SELECT id FROM tb_shop WHERE user_id = #{userId} AND is_deleted = 0")
    Long getShopIdByUserId(@Param("userId") Long userId);
}