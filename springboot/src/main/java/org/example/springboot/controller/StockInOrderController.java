package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.StockInOrder;
import org.example.springboot.service.StockInOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@Tag(name = "入库单接口")
@RestController
@RequestMapping("/stockInOrder")
public class StockInOrderController {
    private static final Logger LOGGER = LoggerFactory.getLogger(StockInOrderController.class);

    @Resource
    private StockInOrderService stockInOrderService;

    @Operation(summary = "创建入库单(含明细)")
    @PostMapping
    public Result<?> create(@RequestBody StockInOrder stockIn) {
        LOGGER.info("创建入库单: itemCount={}", stockIn != null && stockIn.getItems() != null ? stockIn.getItems().size() : 0);
        return Result.success(stockInOrderService.createStockInOrder(stockIn));
    }

    @Operation(summary = "入库单过账(入库存)")
    @PutMapping("/post/{id}")
    public Result<?> post(@PathVariable Long id) {
        stockInOrderService.postStockInOrder(id);
        return Result.success();
    }

    @Operation(summary = "编辑入库单(含明细)")
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody StockInOrder stockIn) {
        stockIn.setId(id);
        return Result.success(stockInOrderService.updateStockInOrder(stockIn));
    }

    @Operation(summary = "获取入库单详情(含明细)")
    @GetMapping("/{id}")
    public Result<?> get(@PathVariable Long id) {
        return Result.success(stockInOrderService.getStockInOrderById(id));
    }

    @Operation(summary = "分页查询入库单")
    @GetMapping("/page")
    public Result<?> page(@RequestParam(required = false) String stockInNo,
                          @RequestParam(required = false) Long acceptanceId,
                          @RequestParam(required = false) Integer status,
                          @RequestParam(defaultValue = "1") Integer currentPage,
                          @RequestParam(defaultValue = "10") Integer size) {
        Page<StockInOrder> page = stockInOrderService.getStockInOrdersByPage(stockInNo, acceptanceId, status, currentPage, size);
        return Result.success(page);
    }

    @Operation(summary = "删除入库单")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        stockInOrderService.deleteStockInOrder(id);
        return Result.success();
    }
}

