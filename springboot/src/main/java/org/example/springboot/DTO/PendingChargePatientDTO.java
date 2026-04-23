package org.example.springboot.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "有待缴费明细的患者（创建缴费单前选择）")
public class PendingChargePatientDTO {
    @Schema(description = "患者ID")
    private Long patientId;
    @Schema(description = "患者姓名")
    private String patientName;
    @Schema(description = "待生成缴费单的明细行数")
    private Integer pendingLineCount;
}
