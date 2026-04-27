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
    @Resource
    private PurchaseOrderMapper purchaseOrderMapper;
    @Resource
    private ListQuerySupport listQuerySupport;

    @Transactional
    public StockInOrder createStockInOrder(StockInOrder stockIn) {
        if (stockIn == null) {
            throw new ServiceException("入库单不能为空");
        }
        if (stockIn.getItems() == null || stockIn.getItems().isEmpty()) {
            throw new ServiceException("入库明细不能为空");
        }

        Set<Long> headerAcceptanceIds = resolveAcceptanceIdsFromItems(stockIn.getItems());
        validateAcceptancesCompleted(headerAcceptanceIds);
        assertNoDraftStockInForAcceptances(headerAcceptanceIds);
        Long headerAcceptanceId = headerAcceptanceIds.size() == 1 ? headerAcceptanceIds.iterator().next() : null;
        stockIn.setAcceptanceId(headerAcceptanceId);

        LocalDateTime now = LocalDateTime.now();
        if (StringUtils.isNotBlank(stockIn.getStockInNo())) {
            if (!DocumentNoHelper.matchesPrefixedDateRandom(stockIn.getStockInNo(), "SI")) {
                throw new ServiceException("入库单号格式无效");
            }
            if (stockInOrderMapper.selectCount(new LambdaQueryWrapper<StockInOrder>()
                    .eq(StockInOrder::getStockInNo, stockIn.getStockInNo())) > 0) {
                throw new ServiceException("入库单号已存在");
            }
        } else {
            stockIn.setStockInNo(generateStockInNo());
        }
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

        for (StockInOrderItem item : stockIn.getItems()) {
            if (item.getAcceptanceItemId() == null) {
                throw new ServiceException("入库明细来源验收明细不能为空");
            }
            if (item.getStockInQty() == null || item.getStockInQty() <= 0) {
                throw new ServiceException("入库数量必须大于0");
            }
            PurchaseAcceptanceItem accItem = purchaseAcceptanceItemMapper.selectById(item.getAcceptanceItemId());
            if (accItem == null) {
                throw new ServiceException("来源验收明细不存在");
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

    @Transactional
    public StockInOrder updateStockInOrder(StockInOrder stockIn) {
        if (stockIn == null || stockIn.getId() == null) {
            throw new ServiceException("入库单不存在");
        }
        StockInOrder existing = stockInOrderMapper.selectById(stockIn.getId());
        if (existing == null) {
            throw new ServiceException("入库单不存在");
        }
        if (existing.getStatus() == null || existing.getStatus() != 0) {
            throw new ServiceException("入库单已过账，不能编辑");
        }
        if (stockIn.getItems() == null || stockIn.getItems().isEmpty()) {
            throw new ServiceException("入库明细不能为空");
        }

        Set<Long> headerAcceptanceIds = resolveAcceptanceIdsFromItems(stockIn.getItems());
        validateAcceptancesCompleted(headerAcceptanceIds);
        Long headerAcceptanceId = headerAcceptanceIds.size() == 1 ? headerAcceptanceIds.iterator().next() : null;

        LocalDateTime now = LocalDateTime.now();

        stockInOrderItemMapper.delete(new LambdaQueryWrapper<StockInOrderItem>().eq(StockInOrderItem::getStockInId, existing.getId()));

        for (StockInOrderItem item : stockIn.getItems()) {
            if (item.getAcceptanceItemId() == null) {
                throw new ServiceException("入库明细来源验收明细不能为空");
            }
            if (item.getStockInQty() == null || item.getStockInQty() <= 0) {
                throw new ServiceException("入库数量必须大于0");
            }
            PurchaseAcceptanceItem accItem = purchaseAcceptanceItemMapper.selectById(item.getAcceptanceItemId());
            if (accItem == null) {
                throw new ServiceException("来源验收明细不存在");
            }
            int qualified = accItem.getQualifiedQty() == null ? 0 : accItem.getQualifiedQty();
            int already = getAlreadyStockedInQty(accItem.getId());
            int available = qualified - already;
            if (item.getStockInQty() > available) {
                throw new ServiceException("入库数量超过验收合格可入库数量");
            }

            item.setId(null);
            item.setStockInId(existing.getId());
            item.setMedicineId(accItem.getMedicineId());
            BigDecimal unitCost = item.getUnitCost() == null ? BigDecimal.ZERO : item.getUnitCost();
            BigDecimal amount = unitCost.multiply(BigDecimal.valueOf(item.getStockInQty()));
            item.setUnitCost(unitCost);
            item.setAmount(amount);
            item.setCreateTime(now);
            item.setUpdateTime(now);
            if (stockInOrderItemMapper.insert(item) <= 0) {
                throw new ServiceException("入库明细更新失败");
            }
        }

        StockInOrder update = new StockInOrder();
        update.setId(existing.getId());
        update.setAcceptanceId(headerAcceptanceId);
        update.setRemark(stockIn.getRemark());
        update.setUpdateTime(now);
        if (stockInOrderMapper.updateById(update) <= 0) {
            throw new ServiceException("入库单更新失败");
        }

        return getStockInOrderById(existing.getId());
    }

    public StockInOrder getStockInOrderById(Long stockInId) {
        StockInOrder stockIn = stockInOrderMapper.selectById(stockInId);
        if (stockIn == null) {
            throw new ServiceException("入库单不存在");
        }

        List<StockInOrderItem> items = stockInOrderItemMapper.selectList(
                new LambdaQueryWrapper<StockInOrderItem>().eq(StockInOrderItem::getStockInId, stockInId)
        );
        for (StockInOrderItem item : items) {
            Medicine medicine = medicineMapper.selectById(item.getMedicineId());
            item.setMedicine(medicine);
        }
        stockIn.setItems(items);

        Set<Long> accIds = new TreeSet<>();
        for (StockInOrderItem item : items) {
            PurchaseAcceptanceItem pai = purchaseAcceptanceItemMapper.selectById(item.getAcceptanceItemId());
            if (pai != null && pai.getAcceptanceId() != null) {
                accIds.add(pai.getAcceptanceId());
            }
        }
        stockIn.setAcceptanceIds(new ArrayList<>(accIds));
        if (accIds.size() == 1) {
            PurchaseAcceptance acceptance = purchaseAcceptanceMapper.selectById(accIds.iterator().next());
            stockIn.setAcceptance(acceptance);
            stockIn.setSourceAcceptances(null);
        } else if (accIds.isEmpty()) {
            stockIn.setAcceptance(stockIn.getAcceptanceId() != null
                    ? purchaseAcceptanceMapper.selectById(stockIn.getAcceptanceId()) : null);
            stockIn.setSourceAcceptances(null);
        } else {
            stockIn.setAcceptance(null);
            List<PurchaseAcceptance> src = purchaseAcceptanceMapper.selectBatchIds(accIds);
            for (PurchaseAcceptance a : src) {
                PurchaseOrder order = purchaseOrderMapper.selectById(a.getPurchaseOrderId());
                a.setPurchaseOrder(order);
            }
            stockIn.setSourceAcceptances(src);
        }
        return stockIn;
    }

    public Page<StockInOrder> getStockInOrdersByPage(String stockInNo, Long acceptanceId, Integer status,
                                                     String creatorName,
                                                     LocalDate createDateStart,
                                                     LocalDate createDateEnd,
                                                     Integer currentPage, Integer size) {
        List<Long> creatorIds = listQuerySupport.resolveUserIdsByKeyword(creatorName);
        if (creatorIds != null && creatorIds.isEmpty()) {
            return new Page<>(currentPage, size, 0);
        }
        LambdaQueryWrapper<StockInOrder> qw = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(stockInNo)) {
            qw.like(StockInOrder::getStockInNo, stockInNo);
        }
        if (acceptanceId != null) {
            qw.and(w -> w.eq(StockInOrder::getAcceptanceId, acceptanceId)
                    .or()
                    .apply("EXISTS (SELECT 1 FROM stock_in_order_item si INNER JOIN purchase_acceptance_item pai ON si.acceptance_item_id = pai.id WHERE si.stock_in_id = stock_in_order.id AND pai.acceptance_id = {0})",
                            acceptanceId));
        }
        if (creatorIds != null) {
            qw.in(StockInOrder::getOperatorUserId, creatorIds);
        }
        ListQuerySupport.applyCreateTimeDateRange(qw, StockInOrder::getCreateTime, createDateStart, createDateEnd);
        if (status != null) {
            qw.eq(StockInOrder::getStatus, status);
        }
        qw.orderByDesc(StockInOrder::getUpdateTime);
        Page<StockInOrder> page = stockInOrderMapper.selectPage(new Page<>(currentPage, size), qw);
        fillAcceptanceIdsForStockIns(page.getRecords());
        return page;
    }

    private Set<Long> resolveAcceptanceIdsFromItems(List<StockInOrderItem> items) {
        Set<Long> ids = new HashSet<>();
        for (StockInOrderItem item : items) {
            if (item.getAcceptanceItemId() == null) {
                continue;
            }
            PurchaseAcceptanceItem accItem = purchaseAcceptanceItemMapper.selectById(item.getAcceptanceItemId());
            if (accItem == null || accItem.getAcceptanceId() == null) {
                throw new ServiceException("来源验收明细不存在");
            }
            ids.add(accItem.getAcceptanceId());
        }
        if (ids.isEmpty()) {
            throw new ServiceException("无法解析来源验收单");
        }
        return ids;
    }

    private void validateAcceptancesCompleted(Set<Long> acceptanceIds) {
        for (Long acceptanceId : acceptanceIds) {
            PurchaseAcceptance acceptance = purchaseAcceptanceMapper.selectById(acceptanceId);
            if (acceptance == null) {
                throw new ServiceException("来源验收单不存在");
            }
            if (acceptance.getStatus() == null || acceptance.getStatus() != 1) {
                throw new ServiceException("仅已完成的验收单允许入库");
            }
        }
    }

    private void assertNoDraftStockInForAcceptances(Set<Long> acceptanceIds) {
        if (acceptanceIds == null || acceptanceIds.isEmpty()) {
            return;
        }
        for (Long acceptanceId : acceptanceIds) {
            if (acceptanceId == null) {
                continue;
            }
            // 1) 单来源入库单（header.acceptance_id）
            PurchaseAcceptance directDraft = purchaseAcceptanceMapper.selectById(acceptanceId);
            if (directDraft == null) {
                throw new ServiceException("来源验收单不存在");
            }
            long directCount = stockInOrderMapper.selectCount(new LambdaQueryWrapper<StockInOrder>()
                    .eq(StockInOrder::getStatus, 0)
                    .eq(StockInOrder::getAcceptanceId, acceptanceId));
            if (directCount > 0) {
                throw new ServiceException("入库数量超过验收合格可入库数量");
            }

            // 2) 多来源合并入库：通过明细关联验收单
            long viaItemCount = stockInOrderMapper.selectCount(new LambdaQueryWrapper<StockInOrder>()
                    .eq(StockInOrder::getStatus, 0)
                    .apply("EXISTS (SELECT 1 FROM stock_in_order_item si INNER JOIN purchase_acceptance_item pai ON si.acceptance_item_id = pai.id WHERE si.stock_in_id = stock_in_order.id AND pai.acceptance_id = {0})",
                            acceptanceId));
            if (viaItemCount > 0) {
                throw new ServiceException("入库数量超过验收合格可入库数量");
            }
        }
    }

    private void fillAcceptanceIdsForStockIns(List<StockInOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        List<Long> orderIds = orders.stream().map(StockInOrder::getId).filter(Objects::nonNull).distinct().toList();
        if (orderIds.isEmpty()) {
            return;
        }
        List<StockInOrderItem> rows = stockInOrderItemMapper.selectList(
                new LambdaQueryWrapper<StockInOrderItem>().in(StockInOrderItem::getStockInId, orderIds));
        if (rows.isEmpty()) {
            for (StockInOrder o : orders) {
                if (o.getAcceptanceId() != null) {
                    o.setAcceptanceIds(List.of(o.getAcceptanceId()));
                }
            }
            return;
        }
        List<Long> accItemIds = rows.stream().map(StockInOrderItem::getAcceptanceItemId).filter(Objects::nonNull).distinct().toList();
        Map<Long, Long> accItemToAcc = new HashMap<>();
        if (!accItemIds.isEmpty()) {
            List<PurchaseAcceptanceItem> accItems = purchaseAcceptanceItemMapper.selectBatchIds(accItemIds);
            for (PurchaseAcceptanceItem pai : accItems) {
                accItemToAcc.put(pai.getId(), pai.getAcceptanceId());
            }
        }
        Map<Long, Set<Long>> byOrder = new HashMap<>();
        for (StockInOrderItem row : rows) {
            Long accId = accItemToAcc.get(row.getAcceptanceItemId());
            if (accId != null) {
                byOrder.computeIfAbsent(row.getStockInId(), k -> new TreeSet<>()).add(accId);
            }
        }
        for (StockInOrder o : orders) {
            Set<Long> set = byOrder.get(o.getId());
            if (set != null && !set.isEmpty()) {
                o.setAcceptanceIds(new ArrayList<>(set));
            } else if (o.getAcceptanceId() != null) {
                o.setAcceptanceIds(List.of(o.getAcceptanceId()));
            }
        }
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

