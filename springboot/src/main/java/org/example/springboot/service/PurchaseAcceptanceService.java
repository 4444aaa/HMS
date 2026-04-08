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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PurchaseAcceptanceService {
    @Resource
    private PurchaseOrderMapper purchaseOrderMapper;
    @Resource
    private PurchaseOrderItemMapper purchaseOrderItemMapper;
    @Resource
    private PurchaseAcceptanceMapper purchaseAcceptanceMapper;
    @Resource
    private PurchaseAcceptanceItemMapper purchaseAcceptanceItemMapper;
    @Resource
    private MedicineMapper medicineMapper;
    @Resource
    private SupplierMapper supplierMapper;
    @Resource
    private StockInOrderMapper stockInOrderMapper;

    @Transactional
    public PurchaseAcceptance createAcceptance(PurchaseAcceptance acceptance) {
        if (acceptance == null) {
            throw new ServiceException("验收单不能为空");
        }
        if (acceptance.getPurchaseOrderId() == null) {
            throw new ServiceException("来源采购单不能为空");
        }
        if (acceptance.getItems() == null || acceptance.getItems().isEmpty()) {
            throw new ServiceException("验收明细不能为空");
        }

        PurchaseOrder order = purchaseOrderMapper.selectById(acceptance.getPurchaseOrderId());
        if (order == null) {
            throw new ServiceException("来源采购单不存在");
        }
        if (order.getStatus() == null || order.getStatus() != 1) {
            throw new ServiceException("仅已发送的采购单允许创建验收单");
        }

        LocalDateTime now = LocalDateTime.now();
        acceptance.setAcceptanceNo(generateAcceptanceNo());
        acceptance.setStatus(acceptance.getStatus() == null ? 0 : acceptance.getStatus());
        acceptance.setCreateTime(now);
        acceptance.setUpdateTime(now);
        try {
            acceptance.setInspectorUserId(JwtTokenUtils.getCurrentUserId());
        } catch (Exception ignored) {
        }

        if (purchaseAcceptanceMapper.insert(acceptance) <= 0) {
            throw new ServiceException("验收单创建失败");
        }

        for (PurchaseAcceptanceItem item : acceptance.getItems()) {
            if (item.getPurchaseOrderItemId() == null) {
                throw new ServiceException("验收明细来源采购单明细不能为空");
            }
            PurchaseOrderItem orderItem = purchaseOrderItemMapper.selectById(item.getPurchaseOrderItemId());
            if (orderItem == null || !order.getId().equals(orderItem.getOrderId())) {
                throw new ServiceException("来源采购单明细不存在或不匹配");
            }

            int orderedQty = orderItem.getOrderQty() == null ? 0 : orderItem.getOrderQty();
            int receivedQty = item.getReceivedQty() == null ? 0 : item.getReceivedQty();
            int qualifiedQty = item.getQualifiedQty() == null ? 0 : item.getQualifiedQty();

            if (receivedQty < 0 || qualifiedQty < 0) {
                throw new ServiceException("验收数量不能为负数");
            }
            if (receivedQty > orderedQty) {
                throw new ServiceException("到货数量不能超过下单数量");
            }
            if (qualifiedQty > receivedQty) {
                throw new ServiceException("合格数量不能超过到货数量");
            }

            item.setAcceptanceId(acceptance.getId());
            item.setMedicineId(orderItem.getMedicineId());
            item.setOrderedQty(orderedQty);
            item.setCreateTime(now);
            item.setUpdateTime(now);
            if (purchaseAcceptanceItemMapper.insert(item) <= 0) {
                throw new ServiceException("验收明细创建失败");
            }
        }

        return getAcceptanceById(acceptance.getId());
    }

    @Transactional
    public void completeAcceptance(Long acceptanceId) {
        PurchaseAcceptance acceptance = purchaseAcceptanceMapper.selectById(acceptanceId);
        if (acceptance == null) {
            throw new ServiceException("验收单不存在");
        }
        if (acceptance.getStatus() == null || acceptance.getStatus() != 0) {
            throw new ServiceException("仅草稿验收单允许完成");
        }

        PurchaseAcceptance update = new PurchaseAcceptance();
        update.setId(acceptanceId);
        update.setStatus(1);
        update.setAcceptanceTime(LocalDateTime.now());
        update.setUpdateTime(LocalDateTime.now());
        if (purchaseAcceptanceMapper.updateById(update) <= 0) {
            throw new ServiceException("验收单完成失败");
        }

        PurchaseOrder order = purchaseOrderMapper.selectById(acceptance.getPurchaseOrderId());
        if (order != null && (order.getStatus() == null || order.getStatus() != 2)) {
            PurchaseOrder orderUpdate = new PurchaseOrder();
            orderUpdate.setId(order.getId());
            orderUpdate.setStatus(2);
            orderUpdate.setUpdateTime(LocalDateTime.now());
            purchaseOrderMapper.updateById(orderUpdate);
        }
    }

    @Transactional
    public PurchaseAcceptance updateAcceptance(PurchaseAcceptance acceptance) {
        if (acceptance == null || acceptance.getId() == null) {
            throw new ServiceException("验收单不存在");
        }
        PurchaseAcceptance existing = purchaseAcceptanceMapper.selectById(acceptance.getId());
        if (existing == null) {
            throw new ServiceException("验收单不存在");
        }
        if (existing.getStatus() == null || existing.getStatus() != 0) {
            throw new ServiceException("验收单已完成，不能编辑");
        }
        if (acceptance.getPurchaseOrderId() == null) {
            throw new ServiceException("来源采购单不能为空");
        }
        if (acceptance.getItems() == null || acceptance.getItems().isEmpty()) {
            throw new ServiceException("验收明细不能为空");
        }

        PurchaseOrder order = purchaseOrderMapper.selectById(acceptance.getPurchaseOrderId());
        if (order == null || order.getStatus() == null || order.getStatus() != 1) {
            throw new ServiceException("仅已发送的采购单允许编辑验收单");
        }

        LocalDateTime now = LocalDateTime.now();
        PurchaseAcceptance update = new PurchaseAcceptance();
        update.setId(existing.getId());
        update.setPurchaseOrderId(acceptance.getPurchaseOrderId());
        update.setRemark(acceptance.getRemark());
        update.setUpdateTime(now);
        if (purchaseAcceptanceMapper.updateById(update) <= 0) {
            throw new ServiceException("验收单更新失败");
        }

        purchaseAcceptanceItemMapper.delete(
                new LambdaQueryWrapper<PurchaseAcceptanceItem>().eq(PurchaseAcceptanceItem::getAcceptanceId, existing.getId())
        );
        for (PurchaseAcceptanceItem item : acceptance.getItems()) {
            if (item.getPurchaseOrderItemId() == null) {
                throw new ServiceException("验收明细来源采购单明细不能为空");
            }
            PurchaseOrderItem orderItem = purchaseOrderItemMapper.selectById(item.getPurchaseOrderItemId());
            if (orderItem == null || !order.getId().equals(orderItem.getOrderId())) {
                throw new ServiceException("来源采购单明细不存在或不匹配");
            }
            int orderedQty = orderItem.getOrderQty() == null ? 0 : orderItem.getOrderQty();
            int receivedQty = item.getReceivedQty() == null ? 0 : item.getReceivedQty();
            int qualifiedQty = item.getQualifiedQty() == null ? 0 : item.getQualifiedQty();
            if (receivedQty < 0 || qualifiedQty < 0) {
                throw new ServiceException("验收数量不能为负数");
            }
            if (receivedQty > orderedQty) {
                throw new ServiceException("到货数量不能超过下单数量");
            }
            if (qualifiedQty > receivedQty) {
                throw new ServiceException("合格数量不能超过到货数量");
            }
            item.setId(null);
            item.setAcceptanceId(existing.getId());
            item.setMedicineId(orderItem.getMedicineId());
            item.setOrderedQty(orderedQty);
            item.setCreateTime(now);
            item.setUpdateTime(now);
            if (purchaseAcceptanceItemMapper.insert(item) <= 0) {
                throw new ServiceException("验收明细更新失败");
            }
        }
        return getAcceptanceById(existing.getId());
    }

    public PurchaseAcceptance getAcceptanceById(Long acceptanceId) {
        PurchaseAcceptance acceptance = purchaseAcceptanceMapper.selectById(acceptanceId);
        if (acceptance == null) {
            throw new ServiceException("验收单不存在");
        }
        PurchaseOrder order = purchaseOrderMapper.selectById(acceptance.getPurchaseOrderId());
        if (order != null) {
            Supplier supplier = supplierMapper.selectById(order.getSupplierId());
            order.setSupplier(supplier);
        }
        acceptance.setPurchaseOrder(order);

        List<PurchaseAcceptanceItem> items = purchaseAcceptanceItemMapper.selectList(
                new LambdaQueryWrapper<PurchaseAcceptanceItem>().eq(PurchaseAcceptanceItem::getAcceptanceId, acceptanceId)
        );
        for (PurchaseAcceptanceItem item : items) {
            Medicine medicine = medicineMapper.selectById(item.getMedicineId());
            item.setMedicine(medicine);
        }
        acceptance.setItems(items);
        return acceptance;
    }

    public Page<PurchaseAcceptance> getAcceptancesByPage(String acceptanceNo, Long purchaseOrderId, Integer status,
                                                        Integer currentPage, Integer size) {
        LambdaQueryWrapper<PurchaseAcceptance> qw = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(acceptanceNo)) {
            qw.like(PurchaseAcceptance::getAcceptanceNo, acceptanceNo);
        }
        if (purchaseOrderId != null) {
            qw.eq(PurchaseAcceptance::getPurchaseOrderId, purchaseOrderId);
        }
        if (status != null) {
            qw.eq(PurchaseAcceptance::getStatus, status);
        }
        qw.orderByDesc(PurchaseAcceptance::getUpdateTime);
        return purchaseAcceptanceMapper.selectPage(new Page<>(currentPage, size), qw);
    }

    private String generateAcceptanceNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = String.format("%06d", (int) (Math.random() * 1000000));
        return "PA" + dateStr + randomStr;
    }

    @Transactional
    public void deleteAcceptance(Long acceptanceId) {
        PurchaseAcceptance acceptance = purchaseAcceptanceMapper.selectById(acceptanceId);
        if (acceptance == null) {
            throw new ServiceException("验收单不存在");
        }
        if (acceptance.getStatus() == null || acceptance.getStatus() != 0) {
            throw new ServiceException("仅草稿验收单允许删除");
        }
        long stockInCount = stockInOrderMapper.selectCount(
                new LambdaQueryWrapper<StockInOrder>().eq(StockInOrder::getAcceptanceId, acceptanceId)
        );
        if (stockInCount > 0) {
            throw new ServiceException("验收单已生成入库单，不能删除");
        }
        purchaseAcceptanceItemMapper.delete(
                new LambdaQueryWrapper<PurchaseAcceptanceItem>().eq(PurchaseAcceptanceItem::getAcceptanceId, acceptanceId)
        );
        purchaseAcceptanceMapper.deleteById(acceptanceId);
    }
}

