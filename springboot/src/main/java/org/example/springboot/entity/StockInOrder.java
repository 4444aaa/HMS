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
@TableName("stock_in_order")
@Schema(description = "入库单主表")
public class StockInOrder {
    @TableId(type = IdType.AUTO)
    @Schema(description = "入库单ID")
    private Long id;

    @Schema(description = "入库单号")
    private String stockInNo;

    @Schema(description = "来源验收单ID")
    private Long acceptanceId;

    @Schema(description = "入库操作人用户ID")
    private Long operatorUserId;

    @Schema(description = "入库时间")
    private LocalDateTime stockInTime;

    @Schema(description = "状态(0:草稿,1:已过账)")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    @Schema(description = "入库明细")
    private List<StockInOrderItem> items;

    @TableField(exist = false)
    @Schema(description = "来源验收单")
    private PurchaseAcceptance acceptance;
}

