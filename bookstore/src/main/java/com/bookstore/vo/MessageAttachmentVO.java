package com.bookstore.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class MessageAttachmentVO implements Serializable {
    private Long id;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
}