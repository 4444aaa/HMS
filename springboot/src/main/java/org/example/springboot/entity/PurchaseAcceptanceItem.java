package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("purchase_acceptance_item")
@Schema(description = "采购验收单明细")
public class PurchaseAcceptanceItem {
    @TableId(type = IdType.AUTO)
    @Schema(description = "验收明细ID")
    private Long id;

    @Schema(description = "验收单ID")
    private Long acceptanceId;

    @Schema(description = "来源采购单明细ID")
    private Long purchaseOrderItemId;

    @Schema(description = "药品ID")
    private Long medicineId;

    @Schema(description = "下单数量(冗余)")
    private Integer orderedQty;

    @Schema(description = "到货数量")
    private Integer receivedQty;

    @Schema(description = "合格数量")
    private Integer qualifiedQty;

    @Schema(description = "批号")
    private String batchNo;

    @Schema(description = "生产日期")
    private LocalDate productionDate;

    @Schema(description = "有效期")
    private LocalDate expiryDate;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    @Schema(description = "药品信息")
    private Medicine medicine;
}

