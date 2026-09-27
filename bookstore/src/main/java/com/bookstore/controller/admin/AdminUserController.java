package com.bookstore.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookstore.common.ErrorCode;
import com.bookstore.common.Result;
import com.bookstore.entity.User;
import com.bookstore.exception.BusinessException;
import com.bookstore.service.UserService;
import com.bookstore.util.UserContext;
import com.bookstore.vo.UserAdminVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/admin/user")
public class AdminUserController {

    @Autowired
    private UserService userService;

    /**
     * 用户列表（分页）
     */
    @GetMapping("/page")
    public Result<IPage<UserAdminVO>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getIsDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getPhone, keyword)
                    .or().like(User::getEmail, keyword));
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByDesc(User::getCreateTime);

        IPage<User> userPage = userService.page(new Page<>(page, size), wrapper);
        IPage<UserAdminVO> voPage = new Page<>(page, size);
        voPage.setTotal(userPage.getTotal());

        List<UserAdminVO> voList = new ArrayList<>();
        for (User user : userPage.getRecords()) {
            UserAdminVO vo = new UserAdminVO();
            BeanUtils.copyProperties(user, vo);
            voList.add(vo);
        }
        voPage.setRecords(voList);
        return Result.success(voPage);
    }

    /**
     * 用户详情
     */
    @GetMapping("/{id}")
    public Result<UserAdminVO> detail(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        UserAdminVO vo = new UserAdminVO();
        BeanUtils.copyProperties(user, vo);
        return Result.success(vo);
    }

    /**
     * 更新用户状态（正常/冻结）
     * <p>
     * 服务端保护：
     *   1) status 只能是 0 或 1（白名单）
     *   2) 不能冻结自己（防止管理员把自己锁死）
     *   3) 不能冻结任意管理员（role==1）
     *   4) Service 层自动事务
     */
    @PutMapping("/{id}/status")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        // 状态白名单：0 冻结 / 1 正常
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "用户状态只能是 0(冻结) 或 1(正常)");
        }

        User user = userService.getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 禁止冻结自己
        Long currentUserId = UserContext.getUserId();
        if (currentUserId != null && currentUserId.equals(id)) {
            throw new BusinessException(ErrorCode.ORDER_NO_PERMISSION, "不能操作自己的账号状态");
        }

        // 禁止冻结其他管理员（role == 1），防止连环锁死
        if (user.getRole() != null && user.getRole() == 1 && status == 0) {
            throw new BusinessException(ErrorCode.ORDER_NO_PERMISSION, "不能冻结管理员账号");
        }

        user.setStatus(status);
        userService.updateById(user);
        return Result.success();
    }
}