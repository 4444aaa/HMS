package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@TableName("purchase_settlement_order")
@Schema(description = "采购结算单")
public class PurchaseSettlementOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String settlementNo;
    private Long supplierId;
    private BigDecimal totalAmount;
    private Integer status;
    private Long cashierUserId;
    private LocalDateTime settlementTime;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private Supplier supplier;
    @TableField(exist = false)
    private List<PurchaseSettlementDetail> details;
    /** 本结算单涉及的入库单 ID（去重、升序） */
    @TableField(exist = false)
    private List<Long> stockInIds = new ArrayList<>();
}
