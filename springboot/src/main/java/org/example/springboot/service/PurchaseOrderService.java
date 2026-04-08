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
public class PurchaseOrderService {
    @Resource
    private PurchasePlanMapper purchasePlanMapper;
    @Resource
    private PurchasePlanItemMapper purchasePlanItemMapper;
    @Resource
    private PurchaseOrderMapper purchaseOrderMapper;
    @Resource
    private PurchaseOrderItemMapper purchaseOrderItemMapper;
    @Resource
    private PurchaseAcceptanceMapper purchaseAcceptanceMapper;
    @Resource
    private SupplierMapper supplierMapper;
    @Resource
    private MedicineMapper medicineMapper;

    @Transactional
    public PurchaseOrder createOrder(PurchaseOrder order) {
        if (order == null) {
            throw new ServiceException("采购单不能为空");
        }
        if (order.getPlanId() == null) {
            throw new ServiceException("来源采购计划不能为空");
        }
        if (order.getSupplierId() == null) {
            throw new ServiceException("供应商不能为空");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new ServiceException("采购单明细不能为空");
        }

        PurchasePlan plan = purchasePlanMapper.selectById(order.getPlanId());
        if (plan == null) {
            throw new ServiceException("来源采购计划不存在");
        }
        if (plan.getStatus() == null || plan.getStatus() != 1) {
            throw new ServiceException("仅已提交的采购计划允许创建采购单");
        }
        Supplier supplier = supplierMapper.selectById(order.getSupplierId());
        if (supplier == null || (supplier.getStatus() != null && supplier.getStatus() == 0)) {
            throw new ServiceException("供应商不存在或已停用");
        }

        LocalDateTime now = LocalDateTime.now();
        order.setOrderNo(generateOrderNo());
        order.setStatus(order.getStatus() == null ? 0 : order.getStatus());
        order.setCreateTime(now);
        order.setUpdateTime(now);
        try {
            order.setCreatorUserId(JwtTokenUtils.getCurrentUserId());
        } catch (Exception ignored) {
        }

        BigDecimal total = BigDecimal.ZERO;
        if (purchaseOrderMapper.insert(order) <= 0) {
            throw new ServiceException("采购单创建失败");
        }

        for (PurchaseOrderItem item : order.getItems()) {
            if (item.getPlanItemId() == null) {
                throw new ServiceException("明细来源计划项不能为空");
            }
            if (item.getOrderQty() == null || item.getOrderQty() <= 0) {
                throw new ServiceException("下单数量必须大于0");
            }

            PurchasePlanItem planItem = purchasePlanItemMapper.selectById(item.getPlanItemId());
            if (planItem == null || !order.getPlanId().equals(planItem.getPlanId())) {
                throw new ServiceException("来源计划明细不存在或不匹配");
            }
            int purchased = planItem.getPurchasedQty() == null ? 0 : planItem.getPurchasedQty();
            int remaining = planItem.getPlanQty() - purchased;
            if (item.getOrderQty() > remaining) {
                throw new ServiceException("下单数量超过计划剩余量");
            }

            item.setOrderId(order.getId());
            item.setMedicineId(planItem.getMedicineId());
            Medicine medicine = medicineMapper.selectById(item.getMedicineId());
            if (medicine == null) {
                throw new ServiceException("药品不存在");
            }
            if (medicine.getSupplierId() == null || !order.getSupplierId().equals(medicine.getSupplierId())) {
                throw new ServiceException("该药品不属于当前供应商供货范围");
            }

            BigDecimal unitPrice = item.getUnitPrice();
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                unitPrice = medicine.getPrice() == null ? BigDecimal.ZERO : medicine.getPrice();
            }
            BigDecimal amount = unitPrice.multiply(BigDecimal.valueOf(item.getOrderQty()));
            item.setUnitPrice(unitPrice);
            item.setAmount(amount);
            item.setCreateTime(now);
            item.setUpdateTime(now);

            if (purchaseOrderItemMapper.insert(item) <= 0) {
                throw new ServiceException("采购单明细创建失败");
            }

            PurchasePlanItem updatePlanItem = new PurchasePlanItem();
            updatePlanItem.setId(planItem.getId());
            updatePlanItem.setPurchasedQty(purchased + item.getOrderQty());
            updatePlanItem.setUpdateTime(now);
            if (purchasePlanItemMapper.updateById(updatePlanItem) <= 0) {
                throw new ServiceException("采购计划明细占用数量更新失败");
            }

            total = total.add(amount);
        }

        PurchaseOrder update = new PurchaseOrder();
        update.setId(order.getId());
        update.setTotalAmount(total);
        update.setUpdateTime(now);
        purchaseOrderMapper.updateById(update);

        return getOrderById(order.getId());
    }

    @Transactional
    public void sendOrder(Long orderId) {
        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("采购单不存在");
        }
        if (order.getStatus() == null || order.getStatus() != 0) {
            throw new ServiceException("仅草稿采购单允许发送");
        }
        PurchaseOrder update = new PurchaseOrder();
        update.setId(orderId);
        update.setStatus(1);
        update.setUpdateTime(LocalDateTime.now());
        if (purchaseOrderMapper.updateById(update) <= 0) {
            throw new ServiceException("采购单发送失败");
        }
    }

    @Transactional
    public PurchaseOrder updateOrder(PurchaseOrder order) {
        if (order == null || order.getId() == null) {
            throw new ServiceException("采购单不存在");
        }
        PurchaseOrder existing = purchaseOrderMapper.selectById(order.getId());
        if (existing == null) {
            throw new ServiceException("采购单不存在");
        }
        if (existing.getStatus() == null || existing.getStatus() != 0) {
            throw new ServiceException("采购单已发送，不能编辑");
        }
        if (order.getPlanId() == null || order.getSupplierId() == null) {
            throw new ServiceException("采购计划和供应商不能为空");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new ServiceException("采购单明细不能为空");
        }

        PurchasePlan plan = purchasePlanMapper.selectById(order.getPlanId());
        if (plan == null || plan.getStatus() == null || plan.getStatus() != 1) {
            throw new ServiceException("仅已提交的采购计划允许编辑采购单");
        }
        Supplier supplier = supplierMapper.selectById(order.getSupplierId());
        if (supplier == null || (supplier.getStatus() != null && supplier.getStatus() == 0)) {
            throw new ServiceException("供应商不存在或已停用");
        }

        List<PurchaseOrderItem> oldItems = purchaseOrderItemMapper.selectList(
                new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getOrderId, existing.getId())
        );
        for (PurchaseOrderItem oldItem : oldItems) {
            PurchasePlanItem planItem = purchasePlanItemMapper.selectById(oldItem.getPlanItemId());
            if (planItem != null) {
                int purchased = planItem.getPurchasedQty() == null ? 0 : planItem.getPurchasedQty();
                int qty = oldItem.getOrderQty() == null ? 0 : oldItem.getOrderQty();
                PurchasePlanItem updatePlanItem = new PurchasePlanItem();
                updatePlanItem.setId(planItem.getId());
                updatePlanItem.setPurchasedQty(Math.max(0, purchased - qty));
                updatePlanItem.setUpdateTime(LocalDateTime.now());
                purchasePlanItemMapper.updateById(updatePlanItem);
            }
        }
        purchaseOrderItemMapper.delete(new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getOrderId, existing.getId()));

        LocalDateTime now = LocalDateTime.now();
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseOrderItem item : order.getItems()) {
            if (item.getPlanItemId() == null) {
                throw new ServiceException("明细来源计划项不能为空");
            }
            if (item.getOrderQty() == null || item.getOrderQty() <= 0) {
                throw new ServiceException("下单数量必须大于0");
            }
            PurchasePlanItem planItem = purchasePlanItemMapper.selectById(item.getPlanItemId());
            if (planItem == null || !order.getPlanId().equals(planItem.getPlanId())) {
                throw new ServiceException("来源计划明细不存在或不匹配");
            }
            int purchased = planItem.getPurchasedQty() == null ? 0 : planItem.getPurchasedQty();
            int remaining = planItem.getPlanQty() - purchased;
            if (item.getOrderQty() > remaining) {
                throw new ServiceException("下单数量超过计划剩余量");
            }

            item.setId(null);
            item.setOrderId(existing.getId());
            item.setMedicineId(planItem.getMedicineId());
            Medicine medicine = medicineMapper.selectById(item.getMedicineId());
            if (medicine == null) {
                throw new ServiceException("药品不存在");
            }
            if (medicine.getSupplierId() == null || !order.getSupplierId().equals(medicine.getSupplierId())) {
                throw new ServiceException("该药品不属于当前供应商供货范围");
            }

            BigDecimal unitPrice = item.getUnitPrice();
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                unitPrice = medicine.getPrice() == null ? BigDecimal.ZERO : medicine.getPrice();
            }
            BigDecimal amount = unitPrice.multiply(BigDecimal.valueOf(item.getOrderQty()));
            item.setUnitPrice(unitPrice);
            item.setAmount(amount);
            item.setCreateTime(now);
            item.setUpdateTime(now);
            if (purchaseOrderItemMapper.insert(item) <= 0) {
                throw new ServiceException("采购单明细更新失败");
            }

            PurchasePlanItem updatePlanItem = new PurchasePlanItem();
            updatePlanItem.setId(planItem.getId());
            updatePlanItem.setPurchasedQty(purchased + item.getOrderQty());
            updatePlanItem.setUpdateTime(now);
            purchasePlanItemMapper.updateById(updatePlanItem);
            total = total.add(amount);
        }

        PurchaseOrder update = new PurchaseOrder();
        update.setId(existing.getId());
        update.setPlanId(order.getPlanId());
        update.setSupplierId(order.getSupplierId());
        update.setRemark(order.getRemark());
        update.setTotalAmount(total);
        update.setUpdateTime(now);
        if (purchaseOrderMapper.updateById(update) <= 0) {
            throw new ServiceException("采购单更新失败");
        }
        return getOrderById(existing.getId());
    }

    public PurchaseOrder getOrderById(Long orderId) {
        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("采购单不存在");
        }
        Supplier supplier = supplierMapper.selectById(order.getSupplierId());
        order.setSupplier(supplier);
        List<PurchaseOrderItem> items = purchaseOrderItemMapper.selectList(
                new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getOrderId, orderId)
        );
        for (PurchaseOrderItem item : items) {
            Medicine medicine = medicineMapper.selectById(item.getMedicineId());
            item.setMedicine(medicine);
        }
        order.setItems(items);
        return order;
    }

    public Page<PurchaseOrder> getOrdersByPage(String orderNo, Long planId, Long supplierId, Integer status,
                                              Integer currentPage, Integer size) {
        LambdaQueryWrapper<PurchaseOrder> qw = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(orderNo)) {
            qw.like(PurchaseOrder::getOrderNo, orderNo);
        }
        if (planId != null) {
            qw.eq(PurchaseOrder::getPlanId, planId);
        }
        if (supplierId != null) {
            qw.eq(PurchaseOrder::getSupplierId, supplierId);
        }
        if (status != null) {
            qw.eq(PurchaseOrder::getStatus, status);
        }
        qw.orderByDesc(PurchaseOrder::getUpdateTime);
        return purchaseOrderMapper.selectPage(new Page<>(currentPage, size), qw);
    }

    private String generateOrderNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = String.format("%06d", (int) (Math.random() * 1000000));
        return "PO" + dateStr + randomStr;
    }

    @Transactional
    public void deleteOrder(Long orderId) {
        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("采购单不存在");
        }
        if (order.getStatus() == null || order.getStatus() != 0) {
            throw new ServiceException("仅草稿采购单允许删除");
        }
        long acceptanceCount = purchaseAcceptanceMapper.selectCount(
                new LambdaQueryWrapper<PurchaseAcceptance>().eq(PurchaseAcceptance::getPurchaseOrderId, orderId)
        );
        if (acceptanceCount > 0) {
            throw new ServiceException("采购单已产生验收单，不能删除");
        }
        List<PurchaseOrderItem> items = purchaseOrderItemMapper.selectList(
                new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getOrderId, orderId)
        );
        for (PurchaseOrderItem item : items) {
            PurchasePlanItem planItem = purchasePlanItemMapper.selectById(item.getPlanItemId());
            if (planItem != null) {
                int purchased = planItem.getPurchasedQty() == null ? 0 : planItem.getPurchasedQty();
                PurchasePlanItem update = new PurchasePlanItem();
                update.setId(planItem.getId());
                update.setPurchasedQty(Math.max(0, purchased - (item.getOrderQty() == null ? 0 : item.getOrderQty())));
                update.setUpdateTime(LocalDateTime.now());
                purchasePlanItemMapper.updateById(update);
            }
        }
        purchaseOrderItemMapper.delete(new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getOrderId, orderId));
        purchaseOrderMapper.deleteById(orderId);
    }
}

