package org.example.springboot.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "有待缴费明细的处方（用于创建缴费单前选择）")
public class PendingChargePrescriptionDTO {
    @Schema(description = "所属患者ID")
    private Long patientId;
    @Schema(description = "处方ID")
    private Long prescriptionId;
    @Schema(description = "处方编号")
    private String prescriptionNo;
    @Schema(description = "患者姓名")
    private String patientName;
    @Schema(description = "待合并缴费明细行数")
    private Integer pendingLineCount;
}
