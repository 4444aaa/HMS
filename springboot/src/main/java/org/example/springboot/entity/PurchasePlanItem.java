package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("purchase_plan_item")
@Schema(description = "采购计划明细")
public class PurchasePlanItem {
    @TableId(type = IdType.AUTO)
    @Schema(description = "计划明细ID")
    private Long id;

    @Schema(description = "采购计划ID")
    private Long planId;

    @Schema(description = "药品ID")
    private Long medicineId;

    @Schema(description = "计划数量")
    private Integer planQty;

    @Schema(description = "已下单数量(累计)")
    private Integer purchasedQty;

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

