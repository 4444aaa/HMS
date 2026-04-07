package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("purchase_order")
@Schema(description = "采购单主表")
public class PurchaseOrder {
    @TableId(type = IdType.AUTO)
    @Schema(description = "采购单ID")
    private Long id;

    @Schema(description = "采购单号")
    private String orderNo;

    @Schema(description = "来源采购计划ID")
    private Long planId;

    @Schema(description = "供应商ID")
    private Long supplierId;

    @Schema(description = "创建人用户ID")
    private Long creatorUserId;

    @Schema(description = "状态(0:草稿,1:已发送,2:已验收完成)")
    private Integer status;

    @Schema(description = "总金额")
    private BigDecimal totalAmount;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    @Schema(description = "采购单明细")
    private List<PurchaseOrderItem> items;

    @TableField(exist = false)
    @Schema(description = "供应商信息")
    private Supplier supplier;
}

