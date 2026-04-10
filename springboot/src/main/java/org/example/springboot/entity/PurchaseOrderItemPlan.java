package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("purchase_order_item_plan")
@Schema(description = "采购单明细-采购计划明细分摊")
public class PurchaseOrderItemPlan {
    @TableId(type = IdType.AUTO)
    @Schema(description = "分摊ID")
    private Long id;

    @Schema(description = "采购单明细ID")
    private Long orderItemId;

    @Schema(description = "采购计划明细ID")
    private Long planItemId;

    @Schema(description = "分摊下单数量")
    private Integer allocatedQty;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
