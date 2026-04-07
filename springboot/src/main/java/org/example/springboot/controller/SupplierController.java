package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Supplier;
import org.example.springboot.service.SupplierService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@Tag(name = "供应商管理接口")
@RestController
@RequestMapping("/supplier")
public class SupplierController {
    private static final Logger LOGGER = LoggerFactory.getLogger(SupplierController.class);

    @Resource
    private SupplierService supplierService;

    @Operation(summary = "新增供应商")
    @PostMapping
    public Result<?> create(@RequestBody Supplier supplier) {
        LOGGER.info("新增供应商: name={}", supplier != null ? supplier.getName() : null);
        return Result.success(supplierService.createSupplier(supplier));
    }

    @Operation(summary = "更新供应商")
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Supplier supplier) {
        supplierService.updateSupplier(id, supplier);
        return Result.success();
    }

    @Operation(summary = "删除供应商")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return Result.success();
    }

    @Operation(summary = "获取供应商详情")
    @GetMapping("/{id}")
    public Result<?> get(@PathVariable Long id) {
        return Result.success(supplierService.getSupplierById(id));
    }

    @Operation(summary = "分页查询供应商")
    @GetMapping("/page")
    public Result<?> page(@RequestParam(required = false) String name,
                          @RequestParam(required = false) String supplierCode,
                          @RequestParam(required = false) Integer status,
                          @RequestParam(defaultValue = "1") Integer currentPage,
                          @RequestParam(defaultValue = "10") Integer size) {
        Page<Supplier> page = supplierService.getSuppliersByPage(name, supplierCode, status, currentPage, size);
        return Result.success(page);
    }

    @Operation(summary = "获取供应商可供药品ID列表")
    @GetMapping("/{id}/medicineIds")
    public Result<?> getMedicineIds(@PathVariable Long id) {
        return Result.success(supplierService.getMedicineIdsBySupplierId(id));
    }
}

