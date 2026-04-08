package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.PurchaseOrder;
import org.example.springboot.service.PurchaseOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@Tag(name = "采购单接口")
@RestController
@RequestMapping("/purchaseOrder")
public class PurchaseOrderController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PurchaseOrderController.class);

    @Resource
    private PurchaseOrderService purchaseOrderService;

    @Operation(summary = "创建采购单(由计划拆分，含明细)")
    @PostMapping
    public Result<?> create(@RequestBody PurchaseOrder order) {
        LOGGER.info("创建采购单: planId={}, supplierId={}", order != null ? order.getPlanId() : null, order != null ? order.getSupplierId() : null);
        return Result.success(purchaseOrderService.createOrder(order));
    }

    @Operation(summary = "发送采购单")
    @PutMapping("/send/{id}")
    public Result<?> send(@PathVariable Long id) {
        purchaseOrderService.sendOrder(id);
        return Result.success();
    }

    @Operation(summary = "编辑采购单(含明细)")
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody PurchaseOrder order) {
        order.setId(id);
        return Result.success(purchaseOrderService.updateOrder(order));
    }

    @Operation(summary = "获取采购单详情(含明细)")
    @GetMapping("/{id}")
    public Result<?> get(@PathVariable Long id) {
        return Result.success(purchaseOrderService.getOrderById(id));
    }

    @Operation(summary = "分页查询采购单")
    @GetMapping("/page")
    public Result<?> page(@RequestParam(required = false) String orderNo,
                          @RequestParam(required = false) Long planId,
                          @RequestParam(required = false) Long supplierId,
                          @RequestParam(required = false) Integer status,
                          @RequestParam(defaultValue = "1") Integer currentPage,
                          @RequestParam(defaultValue = "10") Integer size) {
        Page<PurchaseOrder> page = purchaseOrderService.getOrdersByPage(orderNo, planId, supplierId, status, currentPage, size);
        return Result.success(page);
    }

    @Operation(summary = "删除采购单")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        purchaseOrderService.deleteOrder(id);
        return Result.success();
    }
}

