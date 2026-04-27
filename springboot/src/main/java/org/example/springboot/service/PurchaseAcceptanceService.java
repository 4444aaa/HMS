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
import java.util.stream.Collectors;

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
    private StockInOrderItemMapper stockInOrderItemMapper;
    @Resource
    private StockInOrderMapper stockInOrderMapper;
    @Resource
    private ListQuerySupport listQuerySupport;

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
        long draftCount = purchaseAcceptanceMapper.selectCount(new LambdaQueryWrapper<PurchaseAcceptance>()
                .eq(PurchaseAcceptance::getPurchaseOrderId, acceptance.getPurchaseOrderId())
                .eq(PurchaseAcceptance::getStatus, 0));
        if (draftCount > 0) {
            throw new ServiceException("该采购单已存在草稿验收单，请先编辑或完成现有草稿后再创建");
        }

        LocalDateTime now = LocalDateTime.now();
        if (StringUtils.isNotBlank(acceptance.getAcceptanceNo())) {
            if (!DocumentNoHelper.matchesPrefixedDateRandom(acceptance.getAcceptanceNo(), "PA")) {
                throw new ServiceException("验收单号格式无效");
            }
            if (purchaseAcceptanceMapper.selectCount(new LambdaQueryWrapper<PurchaseAcceptance>()
                    .eq(PurchaseAcceptance::getAcceptanceNo, acceptance.getAcceptanceNo())) > 0) {
                throw new ServiceException("验收单号已存在");
            }
        } else {
            acceptance.setAcceptanceNo(generateAcceptanceNo());
        }
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
            item.setBatchNo(null);
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
            item.setBatchNo(null);
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
                                                        String supplierName,
                                                        String creatorName,
                                                        LocalDate createDateStart,
                                                        LocalDate createDateEnd,
                                                        Integer currentPage, Integer size,
                                                        Boolean excludeFullyStockPosted,
                                                        List<Long> alwaysIncludeAcceptanceIds) {
        List<Long> creatorIds = listQuerySupport.resolveUserIdsByKeyword(creatorName);
        if (creatorIds != null && creatorIds.isEmpty()) {
            return new Page<>(currentPage, size, 0);
        }
        List<Long> supplierIds = listQuerySupport.resolveSupplierIdsByName(supplierName);
        List<Long> orderIdsBySupplier = null;
        if (supplierIds != null) {
            if (supplierIds.isEmpty()) {
                return new Page<>(currentPage, size, 0);
            }
            orderIdsBySupplier = purchaseOrderMapper.selectList(
                            new LambdaQueryWrapper<PurchaseOrder>().in(PurchaseOrder::getSupplierId, supplierIds))
                    .stream().map(PurchaseOrder::getId).filter(Objects::nonNull).distinct().toList();
            if (orderIdsBySupplier.isEmpty()) {
                return new Page<>(currentPage, size, 0);
            }
        }
        boolean creatorFilterActive = creatorIds != null;
        LambdaQueryWrapper<PurchaseAcceptance> qw = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(acceptanceNo)) {
            qw.like(PurchaseAcceptance::getAcceptanceNo, acceptanceNo);
        }
        if (purchaseOrderId != null) {
            qw.eq(PurchaseAcceptance::getPurchaseOrderId, purchaseOrderId);
        }
        if (orderIdsBySupplier != null) {
            qw.in(PurchaseAcceptance::getPurchaseOrderId, orderIdsBySupplier);
        }
        if (creatorIds != null) {
            qw.in(PurchaseAcceptance::getInspectorUserId, creatorIds);
        }
        ListQuerySupport.applyCreateTimeDateRange(qw, PurchaseAcceptance::getCreateTime, createDateStart, createDateEnd);
        if (status != null) {
            qw.eq(PurchaseAcceptance::getStatus, status);
        }
        qw.orderByDesc(PurchaseAcceptance::getUpdateTime);
        if (!Boolean.TRUE.equals(excludeFullyStockPosted)) {
            Page<PurchaseAcceptance> page = purchaseAcceptanceMapper.selectPage(new Page<>(currentPage, size), qw);
            attachOrdersToAcceptances(page.getRecords());
            return page;
        }
        List<PurchaseAcceptance> all = purchaseAcceptanceMapper.selectList(qw);
        if (all.isEmpty()) {
            return new Page<>(currentPage, size, 0);
        }
        List<Long> allAccIds = all.stream().map(PurchaseAcceptance::getId).filter(Objects::nonNull).toList();
        Set<Long> withStockRemaining = acceptanceIdsWithUnpostedQualified(allAccIds);
        Set<Long> always = alwaysIncludeAcceptanceIds == null ? Set.of() : alwaysIncludeAcceptanceIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<PurchaseAcceptance> filtered = all.stream()
                .filter(a -> withStockRemaining.contains(a.getId()) || always.contains(a.getId()))
                .collect(Collectors.toCollection(ArrayList::new));
        Set<Long> seen = filtered.stream().map(PurchaseAcceptance::getId).collect(Collectors.toSet());
        for (Long id : always) {
            if (id != null && !seen.contains(id)) {
                PurchaseAcceptance a = purchaseAcceptanceMapper.selectById(id);
                if (a != null && matchesAcceptanceListFilters(a, acceptanceNo, purchaseOrderId, status,
                        orderIdsBySupplier, creatorFilterActive, creatorIds, createDateStart, createDateEnd)) {
                    filtered.add(a);
                    seen.add(id);
                }
            }
        }
        filtered.sort((x, y) -> {
            LocalDateTime tx = x.getUpdateTime();
            LocalDateTime ty = y.getUpdateTime();
            if (tx == null && ty == null) {
                return 0;
            }
            if (tx == null) {
                return 1;
            }
            if (ty == null) {
                return -1;
            }
            return ty.compareTo(tx);
        });
        long total = filtered.size();
        int from = Math.max(0, (currentPage - 1) * size);
        int to = Math.min(from + size, filtered.size());
        List<PurchaseAcceptance> slice = from < filtered.size() ? filtered.subList(from, to) : List.of();
        Page<PurchaseAcceptance> page = new Page<>(currentPage, size, total);
        page.setRecords(slice);
        attachOrdersToAcceptances(page.getRecords());
        return page;
    }

    private void attachOrdersToAcceptances(List<PurchaseAcceptance> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (PurchaseAcceptance acceptance : records) {
            PurchaseOrder order = purchaseOrderMapper.selectById(acceptance.getPurchaseOrderId());
            if (order != null) {
                Supplier supplier = supplierMapper.selectById(order.getSupplierId());
                order.setSupplier(supplier);
            }
            acceptance.setPurchaseOrder(order);
        }
    }

    private boolean matchesAcceptanceListFilters(PurchaseAcceptance a, String acceptanceNo, Long purchaseOrderId, Integer status,
                                                 List<Long> orderIdsBySupplier,
                                                 boolean creatorFilterActive, List<Long> creatorUserIds,
                                                 LocalDate createDateStart, LocalDate createDateEnd) {
        if (status != null && !Objects.equals(a.getStatus(), status)) {
            return false;
        }
        if (purchaseOrderId != null && !Objects.equals(a.getPurchaseOrderId(), purchaseOrderId)) {
            return false;
        }
        if (StringUtils.isNotBlank(acceptanceNo) && (a.getAcceptanceNo() == null || !a.getAcceptanceNo().contains(acceptanceNo.trim()))) {
            return false;
        }
        if (orderIdsBySupplier != null && (a.getPurchaseOrderId() == null || !orderIdsBySupplier.contains(a.getPurchaseOrderId()))) {
            return false;
        }
        if (creatorFilterActive) {
            if (a.getInspectorUserId() == null || creatorUserIds == null || !creatorUserIds.contains(a.getInspectorUserId())) {
                return false;
            }
        }
        if (createDateStart != null) {
            if (a.getCreateTime() == null || a.getCreateTime().toLocalDate().isBefore(createDateStart)) {
                return false;
            }
        }
        if (createDateEnd != null) {
            if (a.getCreateTime() == null || a.getCreateTime().toLocalDate().isAfter(createDateEnd)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 仍存在「合格数量未在已过账入库单中扣完」的验收明细的验收单 ID（仅统计 status=1 的入库单明细）。
     */
    private Set<Long> acceptanceIdsWithUnpostedQualified(List<Long> acceptanceIds) {
        if (acceptanceIds == null || acceptanceIds.isEmpty()) {
            return Set.of();
        }
        List<PurchaseAcceptanceItem> items = purchaseAcceptanceItemMapper.selectList(
                new LambdaQueryWrapper<PurchaseAcceptanceItem>().in(PurchaseAcceptanceItem::getAcceptanceId, acceptanceIds));
        if (items.isEmpty()) {
            return Set.of();
        }
        List<Long> accItemIds = items.stream().map(PurchaseAcceptanceItem::getId).filter(Objects::nonNull).toList();
        Map<Long, Integer> postedByAccItem = sumPostedStockInQtyByAcceptanceItemIds(accItemIds);
        Map<Long, List<PurchaseAcceptanceItem>> byAcc = items.stream().collect(Collectors.groupingBy(PurchaseAcceptanceItem::getAcceptanceId));
        Set<Long> result = new HashSet<>();
        for (Map.Entry<Long, List<PurchaseAcceptanceItem>> e : byAcc.entrySet()) {
            for (PurchaseAcceptanceItem it : e.getValue()) {
                int q = it.getQualifiedQty() == null ? 0 : it.getQualifiedQty();
                if (q <= 0) {
                    continue;
                }
                int posted = postedByAccItem.getOrDefault(it.getId(), 0);
                if (posted < q) {
                    result.add(e.getKey());
                    break;
                }
            }
        }
        return result;
    }

    private Map<Long, Integer> sumPostedStockInQtyByAcceptanceItemIds(List<Long> acceptanceItemIds) {
        if (acceptanceItemIds == null || acceptanceItemIds.isEmpty()) {
            return Map.of();
        }
        List<StockInOrderItem> lines = stockInOrderItemMapper.selectList(
                new LambdaQueryWrapper<StockInOrderItem>().in(StockInOrderItem::getAcceptanceItemId, acceptanceItemIds));
        if (lines.isEmpty()) {
            return Map.of();
        }
        Set<Long> stockInIds = lines.stream().map(StockInOrderItem::getStockInId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (stockInIds.isEmpty()) {
            return Map.of();
        }
        List<StockInOrder> orders = stockInOrderMapper.selectBatchIds(stockInIds);
        Set<Long> postedStockInIds = orders.stream()
                .filter(o -> o.getStatus() != null && o.getStatus() == 1)
                .map(StockInOrder::getId)
                .collect(Collectors.toSet());
        Map<Long, Integer> sum = new HashMap<>();
        for (StockInOrderItem line : lines) {
            if (!postedStockInIds.contains(line.getStockInId())) {
                continue;
            }
            int q = line.getStockInQty() == null ? 0 : line.getStockInQty();
            sum.merge(line.getAcceptanceItemId(), q, Integer::sum);
        }
        return sum;
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
        List<PurchaseAcceptanceItem> accItems = purchaseAcceptanceItemMapper.selectList(
                new LambdaQueryWrapper<PurchaseAcceptanceItem>().eq(PurchaseAcceptanceItem::getAcceptanceId, acceptanceId)
        );
        if (!accItems.isEmpty()) {
            List<Long> accItemIds = accItems.stream().map(PurchaseAcceptanceItem::getId).toList();
            long used = stockInOrderItemMapper.selectCount(
                    new LambdaQueryWrapper<StockInOrderItem>().in(StockInOrderItem::getAcceptanceItemId, accItemIds)
            );
            if (used > 0) {
                throw new ServiceException("验收单明细已关联入库单，不能删除");
            }
        }
        purchaseAcceptanceItemMapper.delete(
                new LambdaQueryWrapper<PurchaseAcceptanceItem>().eq(PurchaseAcceptanceItem::getAcceptanceId, acceptanceId)
        );
        purchaseAcceptanceMapper.deleteById(acceptanceId);
    }
}

