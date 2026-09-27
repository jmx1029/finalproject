package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bookstore.common.ErrorCode;
import com.bookstore.dto.LoginDTO;
import com.bookstore.dto.RegisterDTO;
import com.bookstore.entity.User;
import com.bookstore.exception.BusinessException;
import com.bookstore.mapper.UserMapper;
import com.bookstore.service.UserService;
import com.bookstore.util.JwtUtil;
import com.bookstore.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bookstore.dto.PasswordUpdateDTO;
import com.bookstore.dto.UserUpdateDTO;
import com.bookstore.util.PathUtil;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService  {

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;


    @Override
    public LoginVO login(LoginDTO dto) {
        // 1. 查询用户
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BusinessException(ErrorCode.USERNAME_NOT_FOUND);
        }

        // 2. 校验密码
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        // 3. 检查状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_FROZEN);
        }

        // 4. 生成 Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 5. 封装返回
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        vo.setIsShopOwner(user.getIsShopOwner());
        vo.setShopId(user.getShopId());
        vo.setShopStatus(user.getShopStatus());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO dto) {
        // 1. 检查用户名是否已存在
        long count = this.count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        // 2. 创建用户，密码加密存储
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setRole(0);
        user.setStatus(1);

        this.save(user);
    }

    // ===== 新增：更新个人信息 =====
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long userId, UserUpdateDTO dto) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getAvatar() != null) user.setAvatar(dto.getAvatar());
        this.updateById(user);
    }

    // ===== 新增：修改密码 =====
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Long userId, PasswordUpdateDTO dto) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        // 验证旧密码
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.OLD_PASSWORD_ERROR);
        }
        // 加密新密码
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        this.updateById(user);
    }

    // ===== 新增：上传头像 =====
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateAvatar(Long userId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }

        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        try {
            // 使用 UploadConfig 中注入的 uploadAbsolutePath
            // 注意：这里需要从 Spring 容器获取，后续我们会调整
            // 临时方案：使用 PathUtil
            String uploadDir = PathUtil.resolvePath("./uploads/avatar/");
            Path dirPath = Paths.get(uploadDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            String originalFilename = file.getOriginalFilename();
            String ext = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                ext = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            if (ext.isEmpty()) {
                ext = ".jpg";
            }

            String newFilename = "avatar_" + userId + "_" + System.currentTimeMillis() + ext;
            Path targetPath = dirPath.resolve(newFilename);
            file.transferTo(targetPath.toFile());

            String avatarUrl = "/uploads/avatar/" + newFilename;
            // 更新数据库
            user.setAvatar(avatarUrl);
            this.updateById(user);
            return avatarUrl;

        } catch (IOException e) {
            throw new BusinessException(ErrorCode.AVATAR_UPLOAD_FAIL, "头像上传失败：" + e.getMessage());
        }
    }

}
