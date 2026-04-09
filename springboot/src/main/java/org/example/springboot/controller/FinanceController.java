package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.OutpatientChargeDetail;
import org.example.springboot.entity.OutpatientChargeOrder;
import org.example.springboot.entity.PurchaseSettlementDetail;
import org.example.springboot.entity.PurchaseSettlementOrder;
import org.example.springboot.service.FinanceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "财务管理接口")
@RestController
@RequestMapping("/finance")
public class FinanceController {
    @Resource
    private FinanceService financeService;

    @Operation(summary = "根据已取药处方生成缴费明细")
    @PostMapping("/outpatient-details/generate")
    public Result<?> generateOutpatientDetails() {
        return Result.success(financeService.generateOutpatientChargeDetails());
    }

    @Operation(summary = "分页查询门诊缴费明细")
    @GetMapping("/outpatient-details/page")
    public Result<?> pageOutpatientDetails(@RequestParam(required = false) Long patientId,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(defaultValue = "1") Integer currentPage,
                                           @RequestParam(defaultValue = "10") Integer size) {
        // 自动生成可缴费明细，避免依赖手工按钮触发
        financeService.generateOutpatientChargeDetails();
        Page<OutpatientChargeDetail> page = financeService.pageOutpatientChargeDetails(patientId, status, currentPage, size);
        return Result.success(page);
    }

    @Operation(summary = "根据缴费明细生成缴费单")
    @PostMapping("/outpatient-orders")
    public Result<?> createOutpatientOrder(@RequestBody Map<String, Object> body) {
        List<Integer> ids = (List<Integer>) body.get("detailIds");
        String remark = body.get("remark") == null ? null : String.valueOf(body.get("remark"));
        List<Long> detailIds = ids.stream().map(Integer::longValue).toList();
        OutpatientChargeOrder order = financeService.createOutpatientChargeOrder(detailIds, remark);
        return Result.success(order);
    }

    @Operation(summary = "门诊缴费单支付")
    @PutMapping("/outpatient-orders/pay/{id}")
    public Result<?> payOutpatientOrder(@PathVariable Long id) {
        financeService.payOutpatientChargeOrder(id);
        return Result.success();
    }

    @Operation(summary = "分页查询门诊缴费单")
    @GetMapping("/outpatient-orders/page")
    public Result<?> pageOutpatientOrders(@RequestParam(required = false) Long patientId,
                                          @RequestParam(required = false) Integer status,
                                          @RequestParam(defaultValue = "1") Integer currentPage,
                                          @RequestParam(defaultValue = "10") Integer size) {
        Page<OutpatientChargeOrder> page = financeService.pageOutpatientChargeOrders(patientId, status, currentPage, size);
        return Result.success(page);
    }

    @Operation(summary = "门诊缴费单详情")
    @GetMapping("/outpatient-orders/{id}")
    public Result<?> getOutpatientOrder(@PathVariable Long id) {
        return Result.success(financeService.getOutpatientChargeOrder(id));
    }

    @Operation(summary = "根据已过账入库单生成结算明细")
    @PostMapping("/settlement-details/generate")
    public Result<?> generateSettlementDetails() {
        return Result.success(financeService.generatePurchaseSettlementDetails());
    }

    @Operation(summary = "分页查询采购结算明细")
    @GetMapping("/settlement-details/page")
    public Result<?> pageSettlementDetails(@RequestParam(required = false) Long supplierId,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(defaultValue = "1") Integer currentPage,
                                           @RequestParam(defaultValue = "10") Integer size) {
        // 自动生成可结算明细，避免依赖手工按钮触发
        financeService.generatePurchaseSettlementDetails();
        Page<PurchaseSettlementDetail> page = financeService.pagePurchaseSettlementDetails(supplierId, status, currentPage, size);
        return Result.success(page);
    }

    @Operation(summary = "根据结算明细生成结算单")
    @PostMapping("/settlement-orders")
    public Result<?> createSettlementOrder(@RequestBody Map<String, Object> body) {
        List<Integer> ids = (List<Integer>) body.get("detailIds");
        String remark = body.get("remark") == null ? null : String.valueOf(body.get("remark"));
        List<Long> detailIds = ids.stream().map(Integer::longValue).toList();
        PurchaseSettlementOrder order = financeService.createPurchaseSettlementOrder(detailIds, remark);
        return Result.success(order);
    }

    @Operation(summary = "采购结算单结算")
    @PutMapping("/settlement-orders/settle/{id}")
    public Result<?> settleOrder(@PathVariable Long id) {
        financeService.settlePurchaseSettlementOrder(id);
        return Result.success();
    }

    @Operation(summary = "分页查询采购结算单")
    @GetMapping("/settlement-orders/page")
    public Result<?> pageSettlementOrders(@RequestParam(required = false) Long supplierId,
                                          @RequestParam(required = false) Integer status,
                                          @RequestParam(defaultValue = "1") Integer currentPage,
                                          @RequestParam(defaultValue = "10") Integer size) {
        Page<PurchaseSettlementOrder> page = financeService.pagePurchaseSettlementOrders(supplierId, status, currentPage, size);
        return Result.success(page);
    }

    @Operation(summary = "采购结算单详情")
    @GetMapping("/settlement-orders/{id}")
    public Result<?> getSettlementOrder(@PathVariable Long id) {
        return Result.success(financeService.getPurchaseSettlementOrder(id));
    }
}
