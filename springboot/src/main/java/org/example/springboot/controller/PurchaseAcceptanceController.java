package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.PurchaseAcceptance;
import org.example.springboot.service.PurchaseAcceptanceImageCreateService;
import org.example.springboot.service.PurchaseAcceptanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Tag(name = "采购验收接口")
@RestController
@RequestMapping("/purchaseAcceptance")
public class PurchaseAcceptanceController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PurchaseAcceptanceController.class);

    @Resource
    private PurchaseAcceptanceService purchaseAcceptanceService;
    @Resource
    private PurchaseAcceptanceImageCreateService purchaseAcceptanceImageCreateService;

    @Operation(summary = "创建验收单(含明细)")
    @PostMapping
    public Result<?> create(@RequestBody PurchaseAcceptance acceptance) {
        LOGGER.info("创建验收单: purchaseOrderId={}", acceptance != null ? acceptance.getPurchaseOrderId() : null);
        return Result.success(purchaseAcceptanceService.createAcceptance(acceptance));
    }

    @Operation(summary = "识图创建验收单")
    @PostMapping("/ocr-create")
    public Result<?> createByOcr(@RequestParam("file") MultipartFile file) {
        return Result.success(purchaseAcceptanceImageCreateService.createByImage(file));
    }

    @Operation(summary = "完成验收单")
    @PutMapping("/complete/{id}")
    public Result<?> complete(@PathVariable Long id) {
        purchaseAcceptanceService.completeAcceptance(id);
        return Result.success();
    }

    @Operation(summary = "编辑验收单(含明细)")
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody PurchaseAcceptance acceptance) {
        acceptance.setId(id);
        return Result.success(purchaseAcceptanceService.updateAcceptance(acceptance));
    }

    @Operation(summary = "获取验收单详情(含明细)")
    @GetMapping("/{id}")
    public Result<?> get(@PathVariable Long id) {
        return Result.success(purchaseAcceptanceService.getAcceptanceById(id));
    }

    @Operation(summary = "分页查询验收单")
    @GetMapping("/page")
    public Result<?> page(@RequestParam(required = false) String acceptanceNo,
                          @RequestParam(required = false) Long purchaseOrderId,
                          @RequestParam(required = false) Integer status,
                          @RequestParam(required = false) String supplierName,
                          @RequestParam(required = false) String creatorName,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createDateStart,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createDateEnd,
                          @RequestParam(defaultValue = "1") Integer currentPage,
                          @RequestParam(defaultValue = "10") Integer size,
                          @RequestParam(required = false) Boolean excludeFullyStockPosted,
                          @RequestParam(required = false) String alwaysIncludeAcceptanceIds) {
        Page<PurchaseAcceptance> page = purchaseAcceptanceService.getAcceptancesByPage(acceptanceNo, purchaseOrderId, status,
                supplierName, creatorName, createDateStart, createDateEnd,
                currentPage, size, excludeFullyStockPosted, parseCommaLongs(alwaysIncludeAcceptanceIds));
        return Result.success(page);
    }

    private static List<Long> parseCommaLongs(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<Long> out = new ArrayList<>();
        for (String part : raw.split(",")) {
            String t = part.trim();
            if (t.isEmpty()) {
                continue;
            }
            try {
                out.add(Long.parseLong(t));
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    @Operation(summary = "删除验收单")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        purchaseAcceptanceService.deleteAcceptance(id);
        return Result.success();
    }
}

