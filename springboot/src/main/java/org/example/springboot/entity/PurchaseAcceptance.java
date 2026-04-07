package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("purchase_acceptance")
@Schema(description = "采购验收单主表")
public class PurchaseAcceptance {
    @TableId(type = IdType.AUTO)
    @Schema(description = "验收单ID")
    private Long id;

    @Schema(description = "验收单号")
    private String acceptanceNo;

    @Schema(description = "来源采购单ID")
    private Long purchaseOrderId;

    @Schema(description = "验收人用户ID")
    private Long inspectorUserId;

    @Schema(description = "验收时间")
    private LocalDateTime acceptanceTime;

    @Schema(description = "状态(0:草稿,1:已完成)")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    @Schema(description = "验收明细")
    private List<PurchaseAcceptanceItem> items;

    @TableField(exist = false)
    @Schema(description = "来源采购单")
    private PurchaseOrder purchaseOrder;
}

