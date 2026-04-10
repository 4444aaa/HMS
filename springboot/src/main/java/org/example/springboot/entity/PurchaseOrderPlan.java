package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("purchase_order_plan")
@Schema(description = "采购单-采购计划关联")
public class PurchaseOrderPlan {
    @TableId(type = IdType.AUTO)
    @Schema(description = "关联ID")
    private Long id;

    @Schema(description = "采购单ID")
    private Long orderId;

    @Schema(description = "采购计划ID")
    private Long planId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
