package com.bookstore.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bookstore.dto.LoginDTO;
import com.bookstore.dto.PasswordUpdateDTO;
import com.bookstore.dto.RegisterDTO;
import com.bookstore.dto.UserUpdateDTO;
import com.bookstore.entity.User;
import com.bookstore.vo.LoginVO;
import org.springframework.web.multipart.MultipartFile;

public interface UserService extends IService<User> {
    LoginVO login(LoginDTO dto);
    void register(RegisterDTO dto);

    // ===== 新增：个人信息管理 =====
    void updateProfile(Long userId, UserUpdateDTO dto);
    void updatePassword(Long userId, PasswordUpdateDTO dto);
    String updateAvatar(Long userId, MultipartFile file);
}