package org.example.springboot.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "有待结算明细的入库单（用于创建结算单前选择）")
public class PendingSettlementStockInDTO {
    @Schema(description = "入库单ID")
    private Long stockInId;
    @Schema(description = "入库单号")
    private String stockInNo;
    @Schema(description = "供应商名称")
    private String supplierName;
    @Schema(description = "待合并结算明细行数")
    private Integer pendingLineCount;
}
