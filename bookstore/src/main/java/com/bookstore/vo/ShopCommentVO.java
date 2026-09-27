package com.bookstore.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ShopCommentVO implements Serializable {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private Long shopId;
    private Long userId;
    private Long orderId;
    private Integer rating;
    private String content;
    private String images;
    private String replyContent;
    private LocalDateTime replyTime;
    private Integer likeCount;
    private String nickname;
    private String username;
    private String avatar;
    private LocalDateTime createTime;
}
