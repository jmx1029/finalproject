package com.bookstore.controller.shop;

import com.bookstore.common.Result;
import com.bookstore.dto.ShopApplyDTO;
import com.bookstore.dto.ShopUpdateDTO;
import com.bookstore.entity.ShopApplication;
import com.bookstore.service.ShopApplicationService;
import com.bookstore.service.ShopService;
import com.bookstore.util.PathUtil;
import com.bookstore.util.UserContext;
import com.bookstore.vo.ShopVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/api/shop")
public class ShopController {

    @Autowired
    private ShopApplicationService shopApplicationService;

    @Autowired
    private ShopService shopService;

    /**
     * 获取当前用户的店铺信息
     */
    @GetMapping("/info")
    public Result<ShopVO> getShopInfo() {
        Long userId = UserContext.getUserId();
        ShopVO shop = shopService.getShopByUserId(userId);
        return Result.success(shop);
    }

    /**
     * 获取当前用户的商家申请状态
     */
    @GetMapping("/application/status")
    public Result<ShopApplication> getApplicationStatus() {
        Long userId = UserContext.getUserId();
        ShopApplication application = shopApplicationService.getLatestApplication(userId);
        return Result.success(application);
    }

    /**
     * 申请成为商家
     */
    @PostMapping("/apply")
    public Result<Void> applyShop(
            @RequestParam String shopName,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String contactPerson,
            @RequestParam(required = false) String contactPhone,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String idCard,
            @RequestParam(required = false) MultipartFile idCardFront,
            @RequestParam(required = false) MultipartFile idCardBack,
            @RequestParam(required = false) MultipartFile businessLicense) throws IOException {

        Long userId = UserContext.getUserId();

        // 1. 处理文件上传（若文件不为空）
        String frontPath = saveFile(idCardFront, "idcard_front", userId);
        String backPath = saveFile(idCardBack, "idcard_back", userId);
        String licensePath = saveFile(businessLicense, "license", userId);

        // 2. 手动组装 DTO（或直接用 Service 层方法，这里为了兼容原有逻辑）
        ShopApplyDTO dto = new ShopApplyDTO();
        dto.setShopName(shopName);
        dto.setDescription(description);
        dto.setContactPerson(contactPerson);
        dto.setContactPhone(contactPhone);
        dto.setAddress(address);
        dto.setIdCard(idCard);
        dto.setIdCardFront(frontPath);
        dto.setIdCardBack(backPath);
        dto.setBusinessLicense(licensePath);

        shopApplicationService.applyShop(userId, dto);
        return Result.success();
    }

    @PutMapping("/update")
    public Result<Void> updateShop(@Valid @RequestBody ShopUpdateDTO dto) {
        Long userId = UserContext.getUserId();
        shopService.updateShop(userId, dto);
        return Result.success();
    }

    /**
     * 保存文件（按用户ID隔离存储）
     */
    private String saveFile(MultipartFile file, String type, Long userId) throws IOException {
        if (file == null || file.isEmpty()) return null;

        // 按用户ID创建子目录：uploads/shop/{userId}/
        String userDir = "shop/" + userId + "/";
        String uploadDir = PathUtil.getSubDir(userDir);

        String ext = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        String filename = type + "_" + System.currentTimeMillis() + ext;
        String fullPath = uploadDir + filename;
        file.transferTo(new File(fullPath));

        return "/uploads/shop/" + userId + "/" + filename;
    }
}