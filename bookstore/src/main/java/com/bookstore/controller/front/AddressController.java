package com.bookstore.controller.front;

import com.bookstore.common.Result;
import com.bookstore.entity.Address;
import com.bookstore.service.AddressService;
import com.bookstore.util.UserContext;
import com.bookstore.vo.AddressVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/address")
public class AddressController {

    @Autowired
    private AddressService addressService;

    /**
     * 获取当前用户的地址列表
     */
    @GetMapping("/list")
    public Result<List<AddressVO>> list() {
        Long userId = UserContext.getUserId();
        return Result.success(addressService.getUserAddresses(userId));
    }

    /**
     * 新增或更新地址
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody Address address) {
        address.setUserId(UserContext.getUserId());
        addressService.saveOrUpdate(address);
        return Result.success();
    }

    /**
     * 设置默认地址
     */
    @PutMapping("/default/{id}")
    public Result<Void> setDefault(@PathVariable Long id) {
        Long userId = UserContext.getUserId();

        // 1. 将所有地址设为非默认
        Address updateAll = new Address();
        updateAll.setIsDefault(0);
        addressService.update(updateAll,
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Address>()
                        .eq(Address::getUserId, userId));

        // 2. 将指定地址设为默认
        Address addr = addressService.getById(id);
        if (addr != null) {
            addr.setIsDefault(1);
            addressService.updateById(addr);
        }
        return Result.success();
    }

    /**
     * 删除地址
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        addressService.removeById(id);
        return Result.success();
    }
}