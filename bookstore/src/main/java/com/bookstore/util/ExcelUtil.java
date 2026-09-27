package com.bookstore.util;

import com.bookstore.entity.Order;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Excel 导出工具类
 * 使用 SXSSFWorkbook 支持大数据量导出
 */
public class ExcelUtil {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 导出订单数据为 Excel
     */
    public static void exportOrders(HttpServletResponse response, List<Order> orders, String filename) throws IOException {
        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + encodedFilename + ".xlsx");

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet("订单数据");

            // 创建样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            // 创建表头
            String[] headers = {
                    "订单号", "用户ID", "收货人", "手机号", "收货地址",
                    "总金额", "状态", "下单时间", "支付时间", "发货时间", "完成时间"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }
            // 调整部分列宽
            sheet.setColumnWidth(0, 6000);
            sheet.setColumnWidth(3, 5000);
            sheet.setColumnWidth(4, 8000);

            // 填充数据
            int rowIndex = 1;
            for (Order order : orders) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(order.getId().toString());
                row.createCell(1).setCellValue(order.getUserId().toString());
                row.createCell(2).setCellValue(order.getReceiverName() != null ? order.getReceiverName() : "");
                row.createCell(3).setCellValue(order.getReceiverPhone() != null ? order.getReceiverPhone() : "");
                row.createCell(4).setCellValue(order.getReceiverAddress() != null ? order.getReceiverAddress() : "");
                row.createCell(5).setCellValue(order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0);
                row.createCell(6).setCellValue(getStatusText(order.getStatus()));

                // 日期字段
                Cell cell7 = row.createCell(7);
                cell7.setCellValue(order.getCreateTime() != null ? order.getCreateTime().format(DATE_FORMATTER) : "");
                cell7.setCellStyle(dateStyle);

                Cell cell8 = row.createCell(8);
                cell8.setCellValue(order.getPayTime() != null ? order.getPayTime().format(DATE_FORMATTER) : "");
                cell8.setCellStyle(dateStyle);

                Cell cell9 = row.createCell(9);
                cell9.setCellValue(order.getShipTime() != null ? order.getShipTime().format(DATE_FORMATTER) : "");
                cell9.setCellStyle(dateStyle);

                Cell cell10 = row.createCell(10);
                cell10.setCellValue(order.getFinishTime() != null ? order.getFinishTime().format(DATE_FORMATTER) : "");
                cell10.setCellStyle(dateStyle);
            }

            workbook.write(response.getOutputStream());
            workbook.dispose();
        }
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private static CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
        return style;
    }

    private static String getStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待付款";
            case 1: return "已支付";
            case 2: return "已发货";
            case 3: return "已完成";
            case 4: return "已取消";
            default: return "未知";
        }
    }
}