package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.springboot.entity.*;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.*;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class StockInOrderService {
    @Resource
    private StockInOrderMapper stockInOrderMapper;
    @Resource
    private StockInOrderItemMapper stockInOrderItemMapper;
    @Resource
    private PurchaseAcceptanceMapper purchaseAcceptanceMapper;
    @Resource
    private PurchaseAcceptanceItemMapper purchaseAcceptanceItemMapper;
    @Resource
    private MedicineMapper medicineMapper;
    @Resource
    private MedicineService medicineService;

    @Transactional
    public StockInOrder createStockInOrder(StockInOrder stockIn) {
        if (stockIn == null) {
            throw new ServiceException("入库单不能为空");
        }
        if (stockIn.getAcceptanceId() == null) {
            throw new ServiceException("来源验收单不能为空");
        }
        if (stockIn.getItems() == null || stockIn.getItems().isEmpty()) {
            throw new ServiceException("入库明细不能为空");
        }

        PurchaseAcceptance acceptance = purchaseAcceptanceMapper.selectById(stockIn.getAcceptanceId());
        if (acceptance == null) {
            throw new ServiceException("来源验收单不存在");
        }
        if (acceptance.getStatus() == null || acceptance.getStatus() != 1) {
            throw new ServiceException("仅已完成的验收单允许创建入库单");
        }

        LocalDateTime now = LocalDateTime.now();
        stockIn.setStockInNo(generateStockInNo());
        stockIn.setStatus(stockIn.getStatus() == null ? 0 : stockIn.getStatus());
        stockIn.setCreateTime(now);
        stockIn.setUpdateTime(now);
        try {
            stockIn.setOperatorUserId(JwtTokenUtils.getCurrentUserId());
        } catch (Exception ignored) {
        }

        if (stockInOrderMapper.insert(stockIn) <= 0) {
            throw new ServiceException("入库单创建失败");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (StockInOrderItem item : stockIn.getItems()) {
            if (item.getAcceptanceItemId() == null) {
                throw new ServiceException("入库明细来源验收明细不能为空");
            }
            if (item.getStockInQty() == null || item.getStockInQty() <= 0) {
                throw new ServiceException("入库数量必须大于0");
            }
            PurchaseAcceptanceItem accItem = purchaseAcceptanceItemMapper.selectById(item.getAcceptanceItemId());
            if (accItem == null || !stockIn.getAcceptanceId().equals(accItem.getAcceptanceId())) {
                throw new ServiceException("来源验收明细不存在或不匹配");
            }

            int qualified = accItem.getQualifiedQty() == null ? 0 : accItem.getQualifiedQty();
            int already = getAlreadyStockedInQty(accItem.getId());
            int available = qualified - already;
            if (item.getStockInQty() > available) {
                throw new ServiceException("入库数量超过验收合格可入库数量");
            }

            item.setStockInId(stockIn.getId());
            item.setMedicineId(accItem.getMedicineId());

            BigDecimal unitCost = item.getUnitCost() == null ? BigDecimal.ZERO : item.getUnitCost();
            BigDecimal amount = unitCost.multiply(BigDecimal.valueOf(item.getStockInQty()));
            item.setUnitCost(unitCost);
            item.setAmount(amount);
            item.setCreateTime(now);
            item.setUpdateTime(now);
            if (stockInOrderItemMapper.insert(item) <= 0) {
                throw new ServiceException("入库明细创建失败");
            }
            total = total.add(amount);
        }

        return getStockInOrderById(stockIn.getId());
    }

    @Transactional
    public void postStockInOrder(Long stockInId) {
        StockInOrder stockIn = stockInOrderMapper.selectById(stockInId);
        if (stockIn == null) {
            throw new ServiceException("入库单不存在");
        }
        if (stockIn.getStatus() == null || stockIn.getStatus() != 0) {
            throw new ServiceException("仅草稿入库单允许过账");
        }

        List<StockInOrderItem> items = stockInOrderItemMapper.selectList(
                new LambdaQueryWrapper<StockInOrderItem>().eq(StockInOrderItem::getStockInId, stockInId)
        );
        if (items == null || items.isEmpty()) {
            throw new ServiceException("入库单明细为空");
        }

        // 再次校验可用数量，避免并发超入库
        for (StockInOrderItem item : items) {
            PurchaseAcceptanceItem accItem = purchaseAcceptanceItemMapper.selectById(item.getAcceptanceItemId());
            if (accItem == null) {
                throw new ServiceException("来源验收明细不存在");
            }
            int qualified = accItem.getQualifiedQty() == null ? 0 : accItem.getQualifiedQty();
            int already = getAlreadyStockedInQty(accItem.getId());
            // already 包含本单吗？此时本单还未过账，但已插入明细，already 会包含本单数量
            // 这里按“本单过账前允许存在”，因此用 qualified - (already - item.qty) 作为可用量
            int availableBeforeThis = qualified - (already - (item.getStockInQty() == null ? 0 : item.getStockInQty()));
            if (item.getStockInQty() > availableBeforeThis) {
                throw new ServiceException("入库数量超过验收合格可入库数量");
            }
        }

        // 过账 + 入库存
        LocalDateTime now = LocalDateTime.now();
        StockInOrder update = new StockInOrder();
        update.setId(stockInId);
        update.setStatus(1);
        update.setStockInTime(now);
        update.setUpdateTime(now);
        if (stockInOrderMapper.updateById(update) <= 0) {
            throw new ServiceException("入库单过账失败");
        }

        for (StockInOrderItem item : items) {
            medicineService.updateStock(item.getMedicineId(), item.getStockInQty());
        }
    }

    public StockInOrder getStockInOrderById(Long stockInId) {
        StockInOrder stockIn = stockInOrderMapper.selectById(stockInId);
        if (stockIn == null) {
            throw new ServiceException("入库单不存在");
        }
        PurchaseAcceptance acceptance = purchaseAcceptanceMapper.selectById(stockIn.getAcceptanceId());
        stockIn.setAcceptance(acceptance);

        List<StockInOrderItem> items = stockInOrderItemMapper.selectList(
                new LambdaQueryWrapper<StockInOrderItem>().eq(StockInOrderItem::getStockInId, stockInId)
        );
        for (StockInOrderItem item : items) {
            Medicine medicine = medicineMapper.selectById(item.getMedicineId());
            item.setMedicine(medicine);
        }
        stockIn.setItems(items);
        return stockIn;
    }

    public Page<StockInOrder> getStockInOrdersByPage(String stockInNo, Long acceptanceId, Integer status,
                                                     Integer currentPage, Integer size) {
        LambdaQueryWrapper<StockInOrder> qw = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(stockInNo)) {
            qw.like(StockInOrder::getStockInNo, stockInNo);
        }
        if (acceptanceId != null) {
            qw.eq(StockInOrder::getAcceptanceId, acceptanceId);
        }
        if (status != null) {
            qw.eq(StockInOrder::getStatus, status);
        }
        qw.orderByDesc(StockInOrder::getUpdateTime);
        return stockInOrderMapper.selectPage(new Page<>(currentPage, size), qw);
    }

    private int getAlreadyStockedInQty(Long acceptanceItemId) {
        List<StockInOrderItem> items = stockInOrderItemMapper.selectList(
                new LambdaQueryWrapper<StockInOrderItem>().eq(StockInOrderItem::getAcceptanceItemId, acceptanceItemId)
        );
        int sum = 0;
        for (StockInOrderItem item : items) {
            sum += item.getStockInQty() == null ? 0 : item.getStockInQty();
        }
        return sum;
    }

    private String generateStockInNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = String.format("%06d", (int) (Math.random() * 1000000));
        return "SI" + dateStr + randomStr;
    }

    @Transactional
    public void deleteStockInOrder(Long stockInId) {
        StockInOrder stockIn = stockInOrderMapper.selectById(stockInId);
        if (stockIn == null) {
            throw new ServiceException("入库单不存在");
        }
        if (stockIn.getStatus() == null || stockIn.getStatus() != 0) {
            throw new ServiceException("仅草稿入库单允许删除");
        }
        stockInOrderItemMapper.delete(new LambdaQueryWrapper<StockInOrderItem>().eq(StockInOrderItem::getStockInId, stockInId));
        stockInOrderMapper.deleteById(stockInId);
    }
}

