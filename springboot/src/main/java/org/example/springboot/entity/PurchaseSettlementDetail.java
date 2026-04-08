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
@TableName("purchase_settlement_detail")
@Schema(description = "采购结算明细")
public class PurchaseSettlementDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long stockInId;
    private Long stockInItemId;
    private Long supplierId;
    private Long medicineId;
    private Integer quantity;
    private BigDecimal unitCost;
    private BigDecimal amount;
    private Integer status;
    private Long settlementOrderId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private Supplier supplier;
    @TableField(exist = false)
    private Medicine medicine;
    @TableField(exist = false)
    private StockInOrder stockInOrder;
}
