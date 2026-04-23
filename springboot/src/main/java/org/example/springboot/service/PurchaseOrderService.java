package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.springboot.entity.*;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.*;
import org.example.springboot.util.DocumentNoHelper;
import org.example.springboot.util.JwtTokenUtils;
import org.example.springboot.util.ListQuerySupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

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
    private PurchaseOrderPlanMapper purchaseOrderPlanMapper;
    @Resource
    private PurchaseOrderItemPlanMapper purchaseOrderItemPlanMapper;
    @Resource
    private PurchaseAcceptanceMapper purchaseAcceptanceMapper;
    @Resource
    private SupplierMapper supplierMapper;
    @Resource
    private MedicineMapper medicineMapper;
    @Resource
    private ListQuerySupport listQuerySupport;

    @Transactional
    public PurchaseOrder createOrder(PurchaseOrder order) {
        if (order == null) {
            throw new ServiceException("采购单不能为空");
        }
        if (order.getSupplierId() == null) {
            throw new ServiceException("供应商不能为空");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new ServiceException("采购单明细不能为空");
        }
        Set<Long> planIds = normalizePlanIds(order);
        Map<Long, PurchasePlanItem> planItemMap = loadAndValidatePlans(planIds);
        Supplier supplier = supplierMapper.selectById(order.getSupplierId());
        if (supplier == null || (supplier.getStatus() != null && supplier.getStatus() == 0)) {
            throw new ServiceException("供应商不存在或已停用");
        }

        LocalDateTime now = LocalDateTime.now();
        order.setPlanId(planIds.iterator().next());
        if (StringUtils.isNotBlank(order.getOrderNo())) {
            if (!DocumentNoHelper.matchesPrefixedDateRandom(order.getOrderNo(), "PO")) {
                throw new ServiceException("采购单号格式无效");
            }
            if (purchaseOrderMapper.selectCount(new LambdaQueryWrapper<PurchaseOrder>()
                    .eq(PurchaseOrder::getOrderNo, order.getOrderNo())) > 0) {
                throw new ServiceException("采购单号已存在");
            }
        } else {
            order.setOrderNo(generateOrderNo());
        }
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
        saveOrderPlanRelations(order.getId(), planIds, now);

        for (PurchaseOrderItem item : order.getItems()) {
            if (item.getOrderQty() == null || item.getOrderQty() <= 0) {
                throw new ServiceException("下单数量必须大于0");
            }
            List<PurchaseOrderItemPlan> allocations = normalizeAllocations(item);
            validateAndApplyAllocations(item, allocations, planItemMap, order.getSupplierId(), now);
            item.setOrderId(order.getId());
            Medicine medicine = medicineMapper.selectById(item.getMedicineId());
            if (medicine == null) {
                throw new ServiceException("药品不存在");
            }
            if (medicine.getSupplierId() == null || !order.getSupplierId().equals(medicine.getSupplierId())) {
                throw new ServiceException("该药品不属于当前供应商供货范围");
            }

            BigDecimal unitPrice = item.getUnitPrice();
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                unitPrice = medicine.getPurchasePrice() == null ? BigDecimal.ZERO : medicine.getPurchasePrice();
            }
            BigDecimal amount = unitPrice.multiply(BigDecimal.valueOf(item.getOrderQty()));
            item.setUnitPrice(unitPrice);
            item.setAmount(amount);
            item.setCreateTime(now);
            item.setUpdateTime(now);

            if (purchaseOrderItemMapper.insert(item) <= 0) {
                throw new ServiceException("采购单明细创建失败");
            }
            saveItemAllocations(item.getId(), allocations, now);

            total = total.add(amount);
        }

        PurchaseOrder update = new PurchaseOrder();
        update.setId(order.getId());
        update.setTotalAmount(total);
        update.setUpdateTime(now);
        purchaseOrderMapper.updateById(update);
        refreshPlanCompletionStatus(planIds);

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
        if (order.getSupplierId() == null) {
            throw new ServiceException("供应商不能为空");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new ServiceException("采购单明细不能为空");
        }
        Set<Long> planIds = normalizePlanIds(order);
        Supplier supplier = supplierMapper.selectById(order.getSupplierId());
        if (supplier == null || (supplier.getStatus() != null && supplier.getStatus() == 0)) {
            throw new ServiceException("供应商不存在或已停用");
        }
        Set<Long> affectedPlanIds = new HashSet<>(loadPlanIdsByOrderId(existing.getId(), existing.getPlanId()));
        rollbackOrderPlanUsage(existing.getId());
        Map<Long, PurchasePlanItem> planItemMap = loadAndValidatePlans(planIds);
        purchaseOrderItemMapper.delete(new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getOrderId, existing.getId()));
        purchaseOrderPlanMapper.delete(new LambdaQueryWrapper<PurchaseOrderPlan>().eq(PurchaseOrderPlan::getOrderId, existing.getId()));

        LocalDateTime now = LocalDateTime.now();
        saveOrderPlanRelations(existing.getId(), planIds, now);
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseOrderItem item : order.getItems()) {
            if (item.getOrderQty() == null || item.getOrderQty() <= 0) {
                throw new ServiceException("下单数量必须大于0");
            }
            List<PurchaseOrderItemPlan> allocations = normalizeAllocations(item);
            validateAndApplyAllocations(item, allocations, planItemMap, order.getSupplierId(), now);
            item.setId(null);
            item.setOrderId(existing.getId());
            Medicine medicine = medicineMapper.selectById(item.getMedicineId());
            if (medicine == null) {
                throw new ServiceException("药品不存在");
            }
            if (medicine.getSupplierId() == null || !order.getSupplierId().equals(medicine.getSupplierId())) {
                throw new ServiceException("该药品不属于当前供应商供货范围");
            }

            BigDecimal unitPrice = item.getUnitPrice();
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                unitPrice = medicine.getPurchasePrice() == null ? BigDecimal.ZERO : medicine.getPurchasePrice();
            }
            BigDecimal amount = unitPrice.multiply(BigDecimal.valueOf(item.getOrderQty()));
            item.setUnitPrice(unitPrice);
            item.setAmount(amount);
            item.setCreateTime(now);
            item.setUpdateTime(now);
            if (purchaseOrderItemMapper.insert(item) <= 0) {
                throw new ServiceException("采购单明细更新失败");
            }
            saveItemAllocations(item.getId(), allocations, now);
            total = total.add(amount);
        }

        PurchaseOrder update = new PurchaseOrder();
        update.setId(existing.getId());
        update.setPlanId(planIds.iterator().next());
        update.setSupplierId(order.getSupplierId());
        update.setRemark(order.getRemark());
        update.setTotalAmount(total);
        update.setUpdateTime(now);
        if (purchaseOrderMapper.updateById(update) <= 0) {
            throw new ServiceException("采购单更新失败");
        }
        affectedPlanIds.addAll(planIds);
        refreshPlanCompletionStatus(affectedPlanIds);
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
        Map<Long, List<PurchaseOrderItemPlan>> allocationMap = new HashMap<>();
        if (!items.isEmpty()) {
            List<PurchaseOrderItemPlan> allocations = purchaseOrderItemPlanMapper.selectList(
                    new LambdaQueryWrapper<PurchaseOrderItemPlan>().in(PurchaseOrderItemPlan::getOrderItemId,
                            items.stream().map(PurchaseOrderItem::getId).collect(Collectors.toList()))
            );
            allocationMap = allocations.stream().collect(Collectors.groupingBy(PurchaseOrderItemPlan::getOrderItemId));
        }
        for (PurchaseOrderItem item : items) {
            Medicine medicine = medicineMapper.selectById(item.getMedicineId());
            item.setMedicine(medicine);
            item.setPlanAllocations(allocationMap.getOrDefault(item.getId(), new ArrayList<>()));
        }
        order.setItems(items);
        List<PurchaseOrderPlan> orderPlans = purchaseOrderPlanMapper.selectList(
                new LambdaQueryWrapper<PurchaseOrderPlan>().eq(PurchaseOrderPlan::getOrderId, orderId)
        );
        if (orderPlans.isEmpty() && order.getPlanId() != null) {
            order.setPlanIds(List.of(order.getPlanId()));
        } else {
            order.setPlanIds(orderPlans.stream().map(PurchaseOrderPlan::getPlanId).distinct().toList());
        }
        return order;
    }

    public Page<PurchaseOrder> getOrdersByPage(String orderNo, Long planId, String supplierName, Integer status,
                                              String creatorName,
                                              LocalDate createDateStart,
                                              LocalDate createDateEnd,
                                              Integer currentPage, Integer size) {
        List<Long> creatorIds = listQuerySupport.resolveUserIdsByKeyword(creatorName);
        if (creatorIds != null && creatorIds.isEmpty()) {
            return new Page<>(currentPage, size, 0);
        }
        List<Long> supplierIds = listQuerySupport.resolveSupplierIdsByName(supplierName);
        if (supplierIds != null && supplierIds.isEmpty()) {
            return new Page<>(currentPage, size, 0);
        }
        LambdaQueryWrapper<PurchaseOrder> qw = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(orderNo)) {
            qw.like(PurchaseOrder::getOrderNo, orderNo);
        }
        if (planId != null) {
            List<Long> orderIds = purchaseOrderPlanMapper.selectList(
                    new LambdaQueryWrapper<PurchaseOrderPlan>().eq(PurchaseOrderPlan::getPlanId, planId)
            ).stream().map(PurchaseOrderPlan::getOrderId).distinct().toList();
            if (orderIds.isEmpty()) {
                qw.eq(PurchaseOrder::getPlanId, planId);
            } else {
                qw.and(w -> w.eq(PurchaseOrder::getPlanId, planId).or().in(PurchaseOrder::getId, orderIds));
            }
        }
        if (supplierIds != null) {
            qw.in(PurchaseOrder::getSupplierId, supplierIds);
        }
        if (creatorIds != null) {
            qw.in(PurchaseOrder::getCreatorUserId, creatorIds);
        }
        ListQuerySupport.applyCreateTimeDateRange(qw, PurchaseOrder::getCreateTime, createDateStart, createDateEnd);
        if (status != null) {
            qw.eq(PurchaseOrder::getStatus, status);
        }
        qw.orderByDesc(PurchaseOrder::getUpdateTime);
        Page<PurchaseOrder> page = purchaseOrderMapper.selectPage(new Page<>(currentPage, size), qw);
        fillPlanIdsForOrders(page.getRecords());
        return page;
    }

    /**
     * 列表页填充关联采购计划 ID（多计划合并一单时 planIds 来自 purchase_order_plan 表）。
     */
    private void fillPlanIdsForOrders(List<PurchaseOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        List<Long> orderIds = orders.stream().map(PurchaseOrder::getId).filter(Objects::nonNull).distinct().toList();
        if (orderIds.isEmpty()) {
            return;
        }
        List<PurchaseOrderPlan> relations = purchaseOrderPlanMapper.selectList(
                new LambdaQueryWrapper<PurchaseOrderPlan>().in(PurchaseOrderPlan::getOrderId, orderIds));
        Map<Long, Set<Long>> planIdsByOrderId = new HashMap<>();
        for (PurchaseOrderPlan rel : relations) {
            planIdsByOrderId
                    .computeIfAbsent(rel.getOrderId(), k -> new TreeSet<>())
                    .add(rel.getPlanId());
        }
        for (PurchaseOrder order : orders) {
            Set<Long> planIds = planIdsByOrderId.get(order.getId());
            if (planIds == null || planIds.isEmpty()) {
                if (order.getPlanId() != null) {
                    order.setPlanIds(List.of(order.getPlanId()));
                }
            } else {
                order.setPlanIds(new ArrayList<>(planIds));
            }
        }
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
        Set<Long> affectedPlanIds = new HashSet<>(loadPlanIdsByOrderId(orderId, order.getPlanId()));
        List<PurchaseOrderItem> items = purchaseOrderItemMapper.selectList(
                new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getOrderId, orderId)
        );
        rollbackOrderPlanUsage(orderId);
        purchaseOrderItemPlanMapper.delete(new LambdaQueryWrapper<PurchaseOrderItemPlan>().in(PurchaseOrderItemPlan::getOrderItemId,
                items.stream().map(PurchaseOrderItem::getId).collect(Collectors.toList())));
        purchaseOrderItemMapper.delete(new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getOrderId, orderId));
        purchaseOrderPlanMapper.delete(new LambdaQueryWrapper<PurchaseOrderPlan>().eq(PurchaseOrderPlan::getOrderId, orderId));
        purchaseOrderMapper.deleteById(orderId);
        refreshPlanCompletionStatus(affectedPlanIds);
    }

    private Set<Long> normalizePlanIds(PurchaseOrder order) {
        Set<Long> planIds = new HashSet<>();
        if (order.getPlanIds() != null) {
            planIds.addAll(order.getPlanIds().stream().filter(id -> id != null).toList());
        }
        if (order.getPlanId() != null) {
            planIds.add(order.getPlanId());
        }
        if (planIds.isEmpty()) {
            throw new ServiceException("来源采购计划不能为空");
        }
        return planIds;
    }

    private Map<Long, PurchasePlanItem> loadAndValidatePlans(Set<Long> planIds) {
        List<PurchasePlan> plans = purchasePlanMapper.selectBatchIds(planIds);
        if (plans.size() != planIds.size()) {
            throw new ServiceException("存在来源采购计划不存在");
        }
        for (PurchasePlan plan : plans) {
            if (plan.getStatus() == null || plan.getStatus() != 1) {
                throw new ServiceException("仅已提交的采购计划允许创建采购单");
            }
        }
        List<PurchasePlanItem> allPlanItems = purchasePlanItemMapper.selectList(
                new LambdaQueryWrapper<PurchasePlanItem>().in(PurchasePlanItem::getPlanId, planIds)
        );
        return allPlanItems.stream().collect(Collectors.toMap(PurchasePlanItem::getId, p -> p));
    }

    private List<PurchaseOrderItemPlan> normalizeAllocations(PurchaseOrderItem item) {
        List<PurchaseOrderItemPlan> allocations = item.getPlanAllocations();
        if ((allocations == null || allocations.isEmpty()) && item.getPlanItemId() != null) {
            PurchaseOrderItemPlan fallback = new PurchaseOrderItemPlan();
            fallback.setPlanItemId(item.getPlanItemId());
            fallback.setAllocatedQty(item.getOrderQty());
            allocations = List.of(fallback);
        }
        if (allocations == null || allocations.isEmpty()) {
            throw new ServiceException("采购单明细必须包含计划明细分摊");
        }
        return allocations;
    }

    private void validateAndApplyAllocations(PurchaseOrderItem item, List<PurchaseOrderItemPlan> allocations,
                                             Map<Long, PurchasePlanItem> planItemMap, Long supplierId, LocalDateTime now) {
        int allocationSum = 0;
        Set<Long> usedPlanItemIds = new HashSet<>();
        Long legacyPlanItemId = null;
        for (PurchaseOrderItemPlan ap : allocations) {
            if (ap.getPlanItemId() == null || ap.getAllocatedQty() == null || ap.getAllocatedQty() <= 0) {
                throw new ServiceException("计划分摊明细不合法");
            }
            if (legacyPlanItemId == null) {
                legacyPlanItemId = ap.getPlanItemId();
            }
            if (!usedPlanItemIds.add(ap.getPlanItemId())) {
                throw new ServiceException("同一计划明细不能重复分摊");
            }
            PurchasePlanItem planItem = planItemMap.get(ap.getPlanItemId());
            if (planItem == null) {
                throw new ServiceException("存在分摊计划明细不属于所选采购计划");
            }
            if (item.getMedicineId() == null) {
                item.setMedicineId(planItem.getMedicineId());
            }
            if (!item.getMedicineId().equals(planItem.getMedicineId())) {
                throw new ServiceException("同一采购单明细只能合并同一种药品");
            }
            int purchased = planItem.getPurchasedQty() == null ? 0 : planItem.getPurchasedQty();
            int remaining = planItem.getPlanQty() - purchased;
            if (ap.getAllocatedQty() > remaining) {
                throw new ServiceException("下单数量超过计划剩余量");
            }
            PurchasePlanItem updatePlanItem = new PurchasePlanItem();
            updatePlanItem.setId(planItem.getId());
            updatePlanItem.setPurchasedQty(purchased + ap.getAllocatedQty());
            updatePlanItem.setUpdateTime(now);
            if (purchasePlanItemMapper.updateById(updatePlanItem) <= 0) {
                throw new ServiceException("采购计划明细占用数量更新失败");
            }
            planItem.setPurchasedQty(purchased + ap.getAllocatedQty());
            allocationSum += ap.getAllocatedQty();
        }
        if (allocationSum != item.getOrderQty()) {
            throw new ServiceException("明细下单数量必须等于分摊数量之和");
        }
        // 兼容旧库结构：purchase_order_item.plan_item_id 仍为 NOT NULL 时写入一个主分摊计划明细
        item.setPlanItemId(legacyPlanItemId);
        Medicine medicine = medicineMapper.selectById(item.getMedicineId());
        if (medicine == null || medicine.getSupplierId() == null || !supplierId.equals(medicine.getSupplierId())) {
            throw new ServiceException("该药品不属于当前供应商供货范围");
        }
    }

    private void saveOrderPlanRelations(Long orderId, Set<Long> planIds, LocalDateTime now) {
        for (Long planId : planIds) {
            PurchaseOrderPlan relation = new PurchaseOrderPlan();
            relation.setOrderId(orderId);
            relation.setPlanId(planId);
            relation.setCreateTime(now);
            purchaseOrderPlanMapper.insert(relation);
        }
    }

    private void saveItemAllocations(Long orderItemId, List<PurchaseOrderItemPlan> allocations, LocalDateTime now) {
        for (PurchaseOrderItemPlan allocation : allocations) {
            PurchaseOrderItemPlan record = new PurchaseOrderItemPlan();
            record.setOrderItemId(orderItemId);
            record.setPlanItemId(allocation.getPlanItemId());
            record.setAllocatedQty(allocation.getAllocatedQty());
            record.setCreateTime(now);
            purchaseOrderItemPlanMapper.insert(record);
        }
    }

    private void rollbackOrderPlanUsage(Long orderId) {
        List<PurchaseOrderItem> oldItems = purchaseOrderItemMapper.selectList(
                new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getOrderId, orderId)
        );
        if (oldItems.isEmpty()) {
            return;
        }
        List<Long> oldItemIds = oldItems.stream().map(PurchaseOrderItem::getId).toList();
        List<PurchaseOrderItemPlan> oldAllocations = purchaseOrderItemPlanMapper.selectList(
                new LambdaQueryWrapper<PurchaseOrderItemPlan>().in(PurchaseOrderItemPlan::getOrderItemId, oldItemIds)
        );
        if (oldAllocations.isEmpty()) {
            for (PurchaseOrderItem oldItem : oldItems) {
                if (oldItem.getPlanItemId() == null) continue;
                PurchasePlanItem planItem = purchasePlanItemMapper.selectById(oldItem.getPlanItemId());
                if (planItem == null) continue;
                int purchased = planItem.getPurchasedQty() == null ? 0 : planItem.getPurchasedQty();
                int qty = oldItem.getOrderQty() == null ? 0 : oldItem.getOrderQty();
                PurchasePlanItem updatePlanItem = new PurchasePlanItem();
                updatePlanItem.setId(planItem.getId());
                updatePlanItem.setPurchasedQty(Math.max(0, purchased - qty));
                updatePlanItem.setUpdateTime(LocalDateTime.now());
                purchasePlanItemMapper.updateById(updatePlanItem);
            }
            return;
        }
        Map<Long, Integer> rollbackByPlanItem = new HashMap<>();
        for (PurchaseOrderItemPlan allocation : oldAllocations) {
            rollbackByPlanItem.merge(allocation.getPlanItemId(), allocation.getAllocatedQty(), Integer::sum);
        }
        for (Map.Entry<Long, Integer> entry : rollbackByPlanItem.entrySet()) {
            PurchasePlanItem planItem = purchasePlanItemMapper.selectById(entry.getKey());
            if (planItem == null) continue;
            int purchased = planItem.getPurchasedQty() == null ? 0 : planItem.getPurchasedQty();
            PurchasePlanItem updatePlanItem = new PurchasePlanItem();
            updatePlanItem.setId(planItem.getId());
            updatePlanItem.setPurchasedQty(Math.max(0, purchased - entry.getValue()));
            updatePlanItem.setUpdateTime(LocalDateTime.now());
            purchasePlanItemMapper.updateById(updatePlanItem);
        }
    }

    private Set<Long> loadPlanIdsByOrderId(Long orderId, Long fallbackPlanId) {
        Set<Long> planIds = purchaseOrderPlanMapper.selectList(
                        new LambdaQueryWrapper<PurchaseOrderPlan>().eq(PurchaseOrderPlan::getOrderId, orderId))
                .stream()
                .map(PurchaseOrderPlan::getPlanId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (planIds.isEmpty() && fallbackPlanId != null) {
            planIds.add(fallbackPlanId);
        }
        return planIds;
    }

    /**
     * 当某计划所有明细「计划量-已下单量」均为0时，置为已完结(2)；否则从已完结恢复为已提交(1)。
     */
    private void refreshPlanCompletionStatus(Set<Long> planIds) {
        if (planIds == null || planIds.isEmpty()) {
            return;
        }
        List<PurchasePlanItem> items = purchasePlanItemMapper.selectList(
                new LambdaQueryWrapper<PurchasePlanItem>().in(PurchasePlanItem::getPlanId, planIds)
        );
        Map<Long, List<PurchasePlanItem>> itemsByPlanId = items.stream()
                .collect(Collectors.groupingBy(PurchasePlanItem::getPlanId));
        LocalDateTime now = LocalDateTime.now();
        for (Long planId : planIds) {
            if (planId == null) {
                continue;
            }
            PurchasePlan plan = purchasePlanMapper.selectById(planId);
            if (plan == null) {
                continue;
            }
            // 草稿状态不参与自动完结/恢复。
            if (Objects.equals(plan.getStatus(), 0)) {
                continue;
            }
            List<PurchasePlanItem> planItems = itemsByPlanId.getOrDefault(planId, List.of());
            boolean completed = !planItems.isEmpty() && planItems.stream().allMatch(it -> {
                int planQty = it.getPlanQty() == null ? 0 : it.getPlanQty();
                int purchasedQty = it.getPurchasedQty() == null ? 0 : it.getPurchasedQty();
                return Math.max(0, planQty - purchasedQty) == 0;
            });
            Integer targetStatus = completed ? 2 : 1;
            if (!Objects.equals(plan.getStatus(), targetStatus)) {
                PurchasePlan update = new PurchasePlan();
                update.setId(planId);
                update.setStatus(targetStatus);
                update.setUpdateTime(now);
                purchasePlanMapper.updateById(update);
            }
        }
    }
}

