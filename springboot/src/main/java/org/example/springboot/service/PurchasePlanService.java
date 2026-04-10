package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.springboot.entity.Medicine;
import org.example.springboot.entity.PurchasePlan;
import org.example.springboot.entity.PurchasePlanItem;
import org.example.springboot.entity.PurchaseOrder;
import org.example.springboot.entity.PurchaseOrderPlan;
import org.example.springboot.entity.Supplier;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.MedicineMapper;
import org.example.springboot.mapper.PurchasePlanItemMapper;
import org.example.springboot.mapper.PurchasePlanMapper;
import org.example.springboot.mapper.PurchaseOrderPlanMapper;
import org.example.springboot.mapper.PurchaseOrderMapper;
import org.example.springboot.mapper.SupplierMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PurchasePlanService {
    @Resource
    private PurchasePlanMapper purchasePlanMapper;

    @Resource
    private PurchasePlanItemMapper purchasePlanItemMapper;

    @Resource
    private MedicineMapper medicineMapper;
    @Resource
    private PurchaseOrderMapper purchaseOrderMapper;
    @Resource
    private SupplierMapper supplierMapper;
    @Resource
    private PurchaseOrderPlanMapper purchaseOrderPlanMapper;

    @Transactional
    public PurchasePlan createPlan(PurchasePlan plan) {
        if (plan == null) {
            throw new ServiceException("采购计划不能为空");
        }
        if (plan.getItems() == null || plan.getItems().isEmpty()) {
            throw new ServiceException("采购计划明细不能为空");
        }

        LocalDateTime now = LocalDateTime.now();
        plan.setPlanNo(generatePlanNo());
        plan.setStatus(plan.getStatus() == null ? 0 : plan.getStatus());
        plan.setCreateTime(now);
        plan.setUpdateTime(now);
        try {
            plan.setCreatorUserId(JwtTokenUtils.getCurrentUserId());
        } catch (Exception ignored) {
        }

        if (purchasePlanMapper.insert(plan) <= 0) {
            throw new ServiceException("采购计划创建失败");
        }

        for (PurchasePlanItem item : plan.getItems()) {
            if (item.getMedicineId() == null) {
                throw new ServiceException("明细药品不能为空");
            }
            if (item.getPlanQty() == null || item.getPlanQty() <= 0) {
                throw new ServiceException("计划数量必须大于0");
            }
            item.setPlanId(plan.getId());
            item.setPurchasedQty(item.getPurchasedQty() == null ? 0 : item.getPurchasedQty());
            item.setCreateTime(now);
            item.setUpdateTime(now);
            if (purchasePlanItemMapper.insert(item) <= 0) {
                throw new ServiceException("采购计划明细创建失败");
            }
        }

        return getPlanById(plan.getId());
    }

    @Transactional
    public void submitPlan(Long planId) {
        PurchasePlan plan = purchasePlanMapper.selectById(planId);
        if (plan == null) {
            throw new ServiceException("采购计划不存在");
        }
        if (plan.getStatus() != null && plan.getStatus() != 0) {
            throw new ServiceException("仅草稿计划允许提交");
        }
        PurchasePlan update = new PurchasePlan();
        update.setId(planId);
        update.setStatus(1);
        update.setUpdateTime(LocalDateTime.now());
        if (purchasePlanMapper.updateById(update) <= 0) {
            throw new ServiceException("采购计划提交失败");
        }
    }

    @Transactional
    public PurchasePlan updatePlan(PurchasePlan plan) {
        if (plan == null || plan.getId() == null) {
            throw new ServiceException("采购计划不存在");
        }
        PurchasePlan existing = purchasePlanMapper.selectById(plan.getId());
        if (existing == null) {
            throw new ServiceException("采购计划不存在");
        }
        if (existing.getStatus() == null || existing.getStatus() != 0) {
            throw new ServiceException("采购计划已提交，不能编辑");
        }
        if (plan.getItems() == null || plan.getItems().isEmpty()) {
            throw new ServiceException("采购计划明细不能为空");
        }

        LocalDateTime now = LocalDateTime.now();
        PurchasePlan update = new PurchasePlan();
        update.setId(existing.getId());
        update.setTitle(plan.getTitle());
        update.setRemark(plan.getRemark());
        update.setUpdateTime(now);
        if (purchasePlanMapper.updateById(update) <= 0) {
            throw new ServiceException("采购计划更新失败");
        }

        purchasePlanItemMapper.delete(new LambdaQueryWrapper<PurchasePlanItem>().eq(PurchasePlanItem::getPlanId, existing.getId()));
        for (PurchasePlanItem item : plan.getItems()) {
            if (item.getMedicineId() == null) {
                throw new ServiceException("明细药品不能为空");
            }
            if (item.getPlanQty() == null || item.getPlanQty() <= 0) {
                throw new ServiceException("计划数量必须大于0");
            }
            item.setId(null);
            item.setPlanId(existing.getId());
            item.setPurchasedQty(0);
            item.setCreateTime(now);
            item.setUpdateTime(now);
            if (purchasePlanItemMapper.insert(item) <= 0) {
                throw new ServiceException("采购计划明细更新失败");
            }
        }
        return getPlanById(existing.getId());
    }

    public PurchasePlan getPlanById(Long planId) {
        PurchasePlan plan = purchasePlanMapper.selectById(planId);
        if (plan == null) {
            throw new ServiceException("采购计划不存在");
        }
        List<PurchasePlanItem> items = purchasePlanItemMapper.selectList(
                new LambdaQueryWrapper<PurchasePlanItem>().eq(PurchasePlanItem::getPlanId, planId)
        );
        for (PurchasePlanItem item : items) {
            Medicine medicine = medicineMapper.selectById(item.getMedicineId());
            item.setMedicine(medicine);
        }
        plan.setItems(items);
        return plan;
    }

    public Page<PurchasePlan> getPlansByPage(String planNo, String title, Integer status,
                                            Integer currentPage, Integer size,
                                            Boolean onlyWithPurchaseRemaining,
                                            List<Long> alwaysIncludePlanIds) {
        LambdaQueryWrapper<PurchasePlan> qw = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(planNo)) {
            qw.like(PurchasePlan::getPlanNo, planNo);
        }
        if (StringUtils.isNotBlank(title)) {
            qw.like(PurchasePlan::getTitle, title);
        }
        if (status != null) {
            qw.eq(PurchasePlan::getStatus, status);
        }
        qw.orderByDesc(PurchasePlan::getUpdateTime);
        if (!Boolean.TRUE.equals(onlyWithPurchaseRemaining)) {
            Page<PurchasePlan> page = purchasePlanMapper.selectPage(new Page<>(currentPage, size), qw);
            return page;
        }
        List<PurchasePlan> all = purchasePlanMapper.selectList(qw);
        if (all.isEmpty()) {
            return new Page<>(currentPage, size, 0);
        }
        List<Long> allIds = all.stream().map(PurchasePlan::getId).filter(Objects::nonNull).toList();
        Set<Long> withRemaining = planIdsWithPurchaseRemaining(allIds);
        Set<Long> always = alwaysIncludePlanIds == null ? Set.of() : alwaysIncludePlanIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<PurchasePlan> filtered = all.stream()
                .filter(p -> withRemaining.contains(p.getId()) || always.contains(p.getId()))
                .collect(Collectors.toCollection(ArrayList::new));
        Set<Long> seen = filtered.stream().map(PurchasePlan::getId).collect(Collectors.toSet());
        for (Long id : always) {
            if (id != null && !seen.contains(id)) {
                PurchasePlan p = purchasePlanMapper.selectById(id);
                if (p != null && matchesPlanListFilters(p, planNo, title, status)) {
                    filtered.add(p);
                    seen.add(id);
                }
            }
        }
        filtered.sort((a, b) -> {
            LocalDateTime ta = a.getUpdateTime();
            LocalDateTime tb = b.getUpdateTime();
            if (ta == null && tb == null) {
                return 0;
            }
            if (ta == null) {
                return 1;
            }
            if (tb == null) {
                return -1;
            }
            return tb.compareTo(ta);
        });
        long total = filtered.size();
        int from = Math.max(0, (currentPage - 1) * size);
        int to = Math.min(from + size, filtered.size());
        List<PurchasePlan> slice = from < filtered.size() ? filtered.subList(from, to) : List.of();
        Page<PurchasePlan> page = new Page<>(currentPage, size, total);
        page.setRecords(slice);
        return page;
    }

    private boolean matchesPlanListFilters(PurchasePlan p, String planNo, String title, Integer status) {
        if (status != null && !Objects.equals(p.getStatus(), status)) {
            return false;
        }
        if (StringUtils.isNotBlank(planNo) && (p.getPlanNo() == null || !p.getPlanNo().contains(planNo.trim()))) {
            return false;
        }
        if (StringUtils.isNotBlank(title) && (p.getTitle() == null || !p.getTitle().contains(title.trim()))) {
            return false;
        }
        return true;
    }

    /** 仍存在「计划量大于已下单量」的明细的计划 ID */
    private Set<Long> planIdsWithPurchaseRemaining(List<Long> planIds) {
        if (planIds == null || planIds.isEmpty()) {
            return Set.of();
        }
        List<PurchasePlanItem> items = purchasePlanItemMapper.selectList(
                new LambdaQueryWrapper<PurchasePlanItem>().in(PurchasePlanItem::getPlanId, planIds));
        Set<Long> result = new HashSet<>();
        for (PurchasePlanItem it : items) {
            int planQty = it.getPlanQty() == null ? 0 : it.getPlanQty();
            int purchased = it.getPurchasedQty() == null ? 0 : it.getPurchasedQty();
            if (planQty > purchased) {
                result.add(it.getPlanId());
            }
        }
        return result;
    }

    private String generatePlanNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = String.format("%06d", (int) (Math.random() * 1000000));
        return "PP" + dateStr + randomStr;
    }

    @Transactional
    public void deletePlan(Long planId) {
        PurchasePlan plan = purchasePlanMapper.selectById(planId);
        if (plan == null) {
            throw new ServiceException("采购计划不存在");
        }
        if (plan.getStatus() == null || plan.getStatus() != 0) {
            throw new ServiceException("仅草稿采购计划允许删除");
        }
        long orderCount = purchaseOrderMapper.selectCount(
                new LambdaQueryWrapper<PurchaseOrder>().eq(PurchaseOrder::getPlanId, planId)
        );
        orderCount += purchaseOrderPlanMapper.selectCount(
                new LambdaQueryWrapper<PurchaseOrderPlan>().eq(PurchaseOrderPlan::getPlanId, planId)
        );
        if (orderCount > 0) {
            throw new ServiceException("采购计划已生成采购单，不能删除");
        }
        purchasePlanItemMapper.delete(new LambdaQueryWrapper<PurchasePlanItem>().eq(PurchasePlanItem::getPlanId, planId));
        purchasePlanMapper.deleteById(planId);
    }

    public List<Supplier> getCandidateSuppliers(Long planId) {
        return getCandidateSuppliersByPlanIds(Collections.singletonList(planId));
    }

    public List<Supplier> getCandidateSuppliersByPlanIds(List<Long> planIds) {
        if (planIds == null || planIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> validPlanIds = planIds.stream().filter(id -> id != null).collect(Collectors.toSet());
        if (validPlanIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<PurchasePlan> plans = purchasePlanMapper.selectBatchIds(validPlanIds);
        if (plans.size() != validPlanIds.size()) {
            throw new ServiceException("存在采购计划不存在");
        }
        List<PurchasePlanItem> items = purchasePlanItemMapper.selectList(
                new LambdaQueryWrapper<PurchasePlanItem>().in(PurchasePlanItem::getPlanId, validPlanIds)
        );
        Set<Long> supplierIds = items.stream()
                .map(PurchasePlanItem::getMedicineId)
                .map(medicineMapper::selectById)
                .filter(m -> m != null && m.getSupplierId() != null)
                .map(Medicine::getSupplierId)
                .collect(Collectors.toSet());
        if (supplierIds.isEmpty()) {
            return Collections.emptyList();
        }
        return supplierMapper.selectBatchIds(supplierIds).stream()
                .filter(s -> s.getStatus() == null || s.getStatus() == 1)
                .toList();
    }
}

