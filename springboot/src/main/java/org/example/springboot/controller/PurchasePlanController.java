package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.PurchasePlan;
import org.example.springboot.service.PurchasePlanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@Tag(name = "采购计划接口")
@RestController
@RequestMapping("/purchasePlan")
public class PurchasePlanController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PurchasePlanController.class);

    @Resource
    private PurchasePlanService purchasePlanService;

    @Operation(summary = "创建采购计划(含明细)")
    @PostMapping
    public Result<?> create(@RequestBody PurchasePlan plan) {
        LOGGER.info("创建采购计划: title={}", plan != null ? plan.getTitle() : null);
        return Result.success(purchasePlanService.createPlan(plan));
    }

    @Operation(summary = "提交采购计划")
    @PutMapping("/submit/{id}")
    public Result<?> submit(@PathVariable Long id) {
        purchasePlanService.submitPlan(id);
        return Result.success();
    }

    @Operation(summary = "获取采购计划详情(含明细)")
    @GetMapping("/{id}")
    public Result<?> get(@PathVariable Long id) {
        return Result.success(purchasePlanService.getPlanById(id));
    }

    @Operation(summary = "获取计划可选供应商列表(由计划明细药品推导)")
    @GetMapping("/{id}/suppliers")
    public Result<?> getCandidateSuppliers(@PathVariable Long id) {
        return Result.success(purchasePlanService.getCandidateSuppliers(id));
    }

    @Operation(summary = "分页查询采购计划")
    @GetMapping("/page")
    public Result<?> page(@RequestParam(required = false) String planNo,
                          @RequestParam(required = false) String title,
                          @RequestParam(required = false) Integer status,
                          @RequestParam(defaultValue = "1") Integer currentPage,
                          @RequestParam(defaultValue = "10") Integer size) {
        Page<PurchasePlan> page = purchasePlanService.getPlansByPage(planNo, title, status, currentPage, size);
        return Result.success(page);
    }

    @Operation(summary = "删除采购计划")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        purchasePlanService.deletePlan(id);
        return Result.success();
    }
}

