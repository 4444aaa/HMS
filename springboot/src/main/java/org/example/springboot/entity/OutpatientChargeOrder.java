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
@TableName("outpatient_charge_order")
@Schema(description = "门诊缴费单")
public class OutpatientChargeOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long patientId;
    private BigDecimal totalAmount;
    private Integer status;
    private Long cashierUserId;
    private LocalDateTime chargeTime;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private Patient patient;
    @TableField(exist = false)
    private List<OutpatientChargeDetail> details;
}
