package com.bookstore.controller.front;

import com.bookstore.common.Result;
import com.bookstore.dto.LoginDTO;
import com.bookstore.dto.RegisterDTO;
import com.bookstore.entity.User;
import com.bookstore.service.UserService;
import com.bookstore.util.UserContext;
import com.bookstore.vo.LoginVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return Result.success();
    }

    @GetMapping("/info")
    public Result<User> getUserInfo() {
        // 从 ThreadLocal 取当前用户ID，再查库返回
        Long userId = UserContext.getUserId();
        User user = userService.getById(userId);
        user.setPassword(null);  // 不返回密码
        return Result.success(user);
    }
}