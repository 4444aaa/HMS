package org.example.springboot.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "有待结算明细的供应商（创建结算单前选择）")
public class PendingSettlementSupplierDTO {
    @Schema(description = "供应商ID")
    private Long supplierId;
    @Schema(description = "供应商名称")
    private String supplierName;
    @Schema(description = "待生成结算单的明细行数")
    private Integer pendingLineCount;
}
