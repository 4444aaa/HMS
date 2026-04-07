package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("stock_in_order_item")
@Schema(description = "入库单明细")
public class StockInOrderItem {
    @TableId(type = IdType.AUTO)
    @Schema(description = "入库明细ID")
    private Long id;

    @Schema(description = "入库单ID")
    private Long stockInId;

    @Schema(description = "来源验收明细ID")
    private Long acceptanceItemId;

    @Schema(description = "药品ID")
    private Long medicineId;

    @Schema(description = "入库数量")
    private Integer stockInQty;

    @Schema(description = "单位成本")
    private BigDecimal unitCost;

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
}

