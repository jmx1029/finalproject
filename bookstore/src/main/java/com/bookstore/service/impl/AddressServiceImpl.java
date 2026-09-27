package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bookstore.entity.Address;
import com.bookstore.mapper.AddressMapper;
import com.bookstore.service.AddressService;
import com.bookstore.vo.AddressVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements AddressService {

    @Override
    public List<AddressVO> getUserAddresses(Long userId) {
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getUserId, userId)
                .eq(Address::getIsDeleted, 0)
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getCreateTime);
        List<Address> list = this.list(wrapper);

        List<AddressVO> result = new ArrayList<>();
        for (Address addr : list) {
            AddressVO vo = new AddressVO();
            BeanUtils.copyProperties(addr, vo);
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateAddress(Long userId, String receiverName, String receiverPhone, String detailAddress) {
        // 1. 查询是否已存在完全相同的地址（同一用户，相同收货人、手机号、详细地址）
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getUserId, userId)
                .eq(Address::getReceiverName, receiverName)
                .eq(Address::getReceiverPhone, receiverPhone)
                .eq(Address::getDetailAddress, detailAddress)
                .eq(Address::getIsDeleted, 0);
        Address exist = this.getOne(wrapper);

        if (exist != null) {
            // 2. 已存在，更新时间（保持活跃）
            exist.setUpdateTime(LocalDateTime.now());
            this.updateById(exist);
        } else {
            // 3. 不存在，新增
            Address address = new Address();
            address.setUserId(userId);
            address.setReceiverName(receiverName);
            address.setReceiverPhone(receiverPhone);
            address.setDetailAddress(detailAddress);
            // 如果该用户还没有任何地址，设为默认
            long count = this.count(new LambdaQueryWrapper<Address>()
                    .eq(Address::getUserId, userId)
                    .eq(Address::getIsDeleted, 0));
            address.setIsDefault(count == 0 ? 1 : 0);
            address.setCreateTime(LocalDateTime.now());
            address.setUpdateTime(LocalDateTime.now());
            address.setIsDeleted(0);
            this.save(address);
        }
    }
}