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
@TableName("purchase_order_item")
@Schema(description = "采购单明细")
public class PurchaseOrderItem {
    @TableId(type = IdType.AUTO)
    @Schema(description = "采购单明细ID")
    private Long id;

    @Schema(description = "采购单ID")
    private Long orderId;

    @Schema(description = "来源计划明细ID")
    private Long planItemId;

    @Schema(description = "药品ID")
    private Long medicineId;

    @Schema(description = "下单数量")
    private Integer orderQty;

    @Schema(description = "单价")
    private BigDecimal unitPrice;

    @Schema(description = "金额")
    private BigDecimal amount;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    @Schema(description = "药品信息")
    private Medicine medicine;

    @TableField(exist = false)
    @Schema(description = "计划明细分摊列表")
    private List<PurchaseOrderItemPlan> planAllocations;
}

