package com.bookstore.controller.front;

import com.bookstore.common.ErrorCode;
import com.bookstore.common.Result;
import com.bookstore.dto.PasswordUpdateDTO;
import com.bookstore.exception.BusinessException;
import com.bookstore.dto.UserUpdateDTO;
import com.bookstore.entity.User;
import com.bookstore.service.UserService;
import com.bookstore.util.UserContext;
import com.bookstore.vo.UserProfileVO;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user")
public class UserProfileController {

    @Autowired
    private UserService userService;

    /**
     * 获取当前用户个人信息
     */
    @GetMapping("/profile")
    public Result<UserProfileVO> getProfile() {
        Long userId = UserContext.getUserId();
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        UserProfileVO vo = new UserProfileVO();
        BeanUtils.copyProperties(user, vo);
        return Result.success(vo);
    }

    /**
     * 更新个人信息
     */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UserUpdateDTO dto) {
        Long userId = UserContext.getUserId();
        userService.updateProfile(userId, dto);
        return Result.success();
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody PasswordUpdateDTO dto) {
        Long userId = UserContext.getUserId();
        userService.updatePassword(userId, dto);
        return Result.success();
    }

    /**
     * 上传头像（直接复用 FileController 的上传逻辑）
     * 但这里我们提供一个专属接口，自动关联到当前用户
     */
    @PostMapping("/avatar")
    public Result<String> updateAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = UserContext.getUserId();
        String avatarUrl = userService.updateAvatar(userId, file);
        return Result.success(avatarUrl);
    }
}