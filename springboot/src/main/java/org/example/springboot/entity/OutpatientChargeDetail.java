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
@TableName("outpatient_charge_detail")
@Schema(description = "门诊缴费明细")
public class OutpatientChargeDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long prescriptionId;
    private Long prescriptionDetailId;
    private Long patientId;
    private Long medicineId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private Integer status;
    private Long chargeOrderId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private Patient patient;
    @TableField(exist = false)
    private Medicine medicine;
    @TableField(exist = false)
    private Prescription prescription;
}
