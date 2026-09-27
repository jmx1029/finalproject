package com.bookstore.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * === 阶段四 T5：SendMessageDTO ===
 * 修复点：
 * 1. 加 @JsonIgnoreProperties(ignoreUnknown = true)，防止前端多传字段（如 hack 字段）被反序列化导致异常
 * 2. messageType 用 @Min(0)/@Max(2) 白名单，只允许 0-文本 / 1-订单卡片 / 2-图片 三种类型
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SendMessageDTO {

    @NotBlank(message = "接收方ID不能为空")
    private String receiverId;

    /**
     * 消息内容
     * 注意：文本消息(messageType=0)时必填，图片/订单卡片(messageType=1/2)时选填
     * 条件校验在 Service 层完成（@Valid 只做长度/非空等通用校验）
     */
    @Size(max = 2000, message = "消息内容长度不能超过 2000 个字符")
    private String content;

    /** 0-文本 1-订单卡片 2-图片（阶段四 T5：加白名单，拒绝其他非法值） */
    @Min(value = 0, message = "消息类型只能是 0(文本)、1(订单卡片) 或 2(图片)")
    @Max(value = 2, message = "消息类型只能是 0(文本)、1(订单卡片) 或 2(图片)")
    private Integer messageType;

    private String orderId;

    private List<String> imageUrls;
}
