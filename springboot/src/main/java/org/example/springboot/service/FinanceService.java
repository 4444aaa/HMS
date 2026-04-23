package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.example.springboot.DTO.PendingChargePatientDTO;
import org.example.springboot.DTO.PendingChargePrescriptionDTO;
import org.example.springboot.DTO.PendingSettlementStockInDTO;
import org.example.springboot.DTO.PendingSettlementSupplierDTO;
import org.example.springboot.entity.*;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.*;
import org.example.springboot.util.JwtTokenUtils;
import org.example.springboot.util.ListQuerySupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class FinanceService {
    @Resource
    private PrescriptionMapper prescriptionMapper;
    @Resource
    private PrescriptionDetailMapper prescriptionDetailMapper;
    @Resource
    private MedicineMapper medicineMapper;
    @Resource
    private PatientMapper patientMapper;
    @Resource
    private OutpatientChargeDetailMapper outpatientChargeDetailMapper;
    @Resource
    private OutpatientChargeOrderMapper outpatientChargeOrderMapper;
    @Resource
    private StockInOrderMapper stockInOrderMapper;
    @Resource
    private StockInOrderItemMapper stockInOrderItemMapper;
    @Resource
    private PurchaseAcceptanceMapper purchaseAcceptanceMapper;
    @Resource
    private PurchaseAcceptanceItemMapper purchaseAcceptanceItemMapper;
    @Resource
    private PurchaseOrderMapper purchaseOrderMapper;
    @Resource
    private SupplierMapper supplierMapper;
    @Resource
    private PurchaseSettlementDetailMapper purchaseSettlementDetailMapper;
    @Resource
    private PurchaseSettlementOrderMapper purchaseSettlementOrderMapper;
    @Resource
    private ListQuerySupport listQuerySupport;

    @Transactional
    public int generateOutpatientChargeDetails() {
        int created = 0;
        List<Prescription> donePrescriptions = prescriptionMapper.selectList(
                new LambdaQueryWrapper<Prescription>().eq(Prescription::getStatus, 2)
        );
        LocalDateTime now = LocalDateTime.now();
        for (Prescription prescription : donePrescriptions) {
            List<PrescriptionDetail> details = prescriptionDetailMapper.selectList(
                    new LambdaQueryWrapper<PrescriptionDetail>().eq(PrescriptionDetail::getPrescriptionId, prescription.getId())
            );
            for (PrescriptionDetail detail : details) {
                Long exists = outpatientChargeDetailMapper.selectCount(
                        new LambdaQueryWrapper<OutpatientChargeDetail>()
                                .eq(OutpatientChargeDetail::getPrescriptionDetailId, detail.getId())
                );
                if (exists != null && exists > 0) {
                    continue;
                }
                Medicine medicine = medicineMapper.selectById(detail.getMedicineId());
                BigDecimal unitPrice = medicine == null || medicine.getSalePrice() == null ? BigDecimal.ZERO : medicine.getSalePrice();
                int quantity = detail.getQuantity() == null ? 0 : detail.getQuantity();

                OutpatientChargeDetail chargeDetail = new OutpatientChargeDetail();
                chargeDetail.setPrescriptionId(prescription.getId());
                chargeDetail.setPrescriptionDetailId(detail.getId());
                chargeDetail.setPatientId(prescription.getPatientId());
                chargeDetail.setMedicineId(detail.getMedicineId());
                chargeDetail.setQuantity(quantity);
                chargeDetail.setUnitPrice(unitPrice);
                chargeDetail.setAmount(unitPrice.multiply(BigDecimal.valueOf(quantity)));
                chargeDetail.setStatus(0);
                chargeDetail.setCreateTime(now);
                chargeDetail.setUpdateTime(now);
                outpatientChargeDetailMapper.insert(chargeDetail);
                created++;
            }
        }
        return created;
    }

    public Page<OutpatientChargeDetail> pageOutpatientChargeDetails(Long patientId, Integer status,
                                                                    List<Long> prescriptionIds,
                                                                    Integer currentPage, Integer size) {
        LambdaQueryWrapper<OutpatientChargeDetail> qw = new LambdaQueryWrapper<>();
        if (patientId != null) {
            qw.eq(OutpatientChargeDetail::getPatientId, patientId);
        }
        if (status != null) {
            qw.eq(OutpatientChargeDetail::getStatus, status);
        }
        if (prescriptionIds != null && !prescriptionIds.isEmpty()) {
            qw.in(OutpatientChargeDetail::getPrescriptionId, prescriptionIds);
        }
        qw.orderByDesc(OutpatientChargeDetail::getUpdateTime);
        Page<OutpatientChargeDetail> page = outpatientChargeDetailMapper.selectPage(new Page<>(currentPage, size), qw);
        fillOutpatientDetails(page.getRecords());
        return page;
    }

    /**
     * 存在「未生成缴费单」明细的患者列表，供创建缴费单前先选择患者。
     */
    public List<PendingChargePatientDTO> listPendingChargePatients() {
        List<OutpatientChargeDetail> pendings = outpatientChargeDetailMapper.selectList(
                new LambdaQueryWrapper<OutpatientChargeDetail>().eq(OutpatientChargeDetail::getStatus, 0)
        );
        Map<Long, Long> countByPatient = pendings.stream()
                .filter(d -> d.getPatientId() != null)
                .collect(Collectors.groupingBy(OutpatientChargeDetail::getPatientId, Collectors.counting()));
        List<Long> sortedIds = new ArrayList<>(countByPatient.keySet());
        sortedIds.sort(Comparator.naturalOrder());
        List<PendingChargePatientDTO> list = new ArrayList<>();
        for (Long pid : sortedIds) {
            Patient patient = patientMapper.selectById(pid);
            PendingChargePatientDTO dto = new PendingChargePatientDTO();
            dto.setPatientId(pid);
            dto.setPatientName(patient != null && StringUtils.isNotBlank(patient.getName()) ? patient.getName() : String.valueOf(pid));
            dto.setPendingLineCount(countByPatient.get(pid).intValue());
            list.add(dto);
        }
        return list;
    }

    /**
     * 存在「未生成缴费单」明细的处方列表；若传入 patientId 则仅返回该患者的处方。
     */
    public List<PendingChargePrescriptionDTO> listPendingChargePrescriptions(Long patientIdFilter) {
        List<OutpatientChargeDetail> pendings = outpatientChargeDetailMapper.selectList(
                new LambdaQueryWrapper<OutpatientChargeDetail>().eq(OutpatientChargeDetail::getStatus, 0)
        );
        Map<Long, Long> countByRx = pendings.stream()
                .filter(d -> d.getPrescriptionId() != null)
                .collect(Collectors.groupingBy(OutpatientChargeDetail::getPrescriptionId, Collectors.counting()));
        List<Long> sortedIds = new ArrayList<>(countByRx.keySet());
        sortedIds.sort(Comparator.naturalOrder());
        List<PendingChargePrescriptionDTO> list = new ArrayList<>();
        for (Long rxId : sortedIds) {
            Prescription p = prescriptionMapper.selectById(rxId);
            if (patientIdFilter != null) {
                if (p == null || p.getPatientId() == null || !patientIdFilter.equals(p.getPatientId())) {
                    continue;
                }
            }
            Patient patient = p != null && p.getPatientId() != null ? patientMapper.selectById(p.getPatientId()) : null;
            PendingChargePrescriptionDTO dto = new PendingChargePrescriptionDTO();
            dto.setPatientId(p != null ? p.getPatientId() : null);
            dto.setPrescriptionId(rxId);
            dto.setPrescriptionNo(p != null && StringUtils.isNotBlank(p.getPrescriptionNo()) ? p.getPrescriptionNo() : String.valueOf(rxId));
            dto.setPatientName(patient != null && StringUtils.isNotBlank(patient.getName()) ? patient.getName() : "-");
            dto.setPendingLineCount(countByRx.get(rxId).intValue());
            list.add(dto);
        }
        return list;
    }

    @Transactional
    public OutpatientChargeOrder createOutpatientChargeOrder(List<Long> detailIds, String remark) {
        if (detailIds == null || detailIds.isEmpty()) {
            throw new ServiceException("请选择要生成缴费单的缴费明细");
        }
        List<OutpatientChargeDetail> details = outpatientChargeDetailMapper.selectBatchIds(detailIds);
        if (details.size() != detailIds.size()) {
            throw new ServiceException("存在无效的缴费明细");
        }

        Long patientId = null;
        BigDecimal total = BigDecimal.ZERO;
        for (OutpatientChargeDetail detail : details) {
            if (detail.getStatus() != null && detail.getStatus() == 1) {
                throw new ServiceException("选中的缴费明细存在已生成单据记录");
            }
            if (patientId == null) {
                patientId = detail.getPatientId();
            } else if (!patientId.equals(detail.getPatientId())) {
                throw new ServiceException("一张缴费单仅允许同一患者的缴费明细");
            }
            total = total.add(detail.getAmount() == null ? BigDecimal.ZERO : detail.getAmount());
        }

        LocalDateTime now = LocalDateTime.now();
        OutpatientChargeOrder order = new OutpatientChargeOrder();
        order.setOrderNo(generateOutpatientOrderNo());
        order.setPatientId(patientId);
        order.setTotalAmount(total);
        order.setStatus(0);
        order.setRemark(remark);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        try {
            order.setCashierUserId(JwtTokenUtils.getCurrentUserId());
        } catch (Exception ignored) {
        }
        outpatientChargeOrderMapper.insert(order);

        for (OutpatientChargeDetail detail : details) {
            OutpatientChargeDetail update = new OutpatientChargeDetail();
            update.setId(detail.getId());
            update.setStatus(1);
            update.setChargeOrderId(order.getId());
            update.setUpdateTime(now);
            outpatientChargeDetailMapper.updateById(update);
        }
        return getOutpatientChargeOrder(order.getId());
    }

    @Transactional
    public void payOutpatientChargeOrder(Long orderId) {
        OutpatientChargeOrder order = outpatientChargeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("缴费单不存在");
        }
        if (order.getStatus() != null && order.getStatus() == 1) {
            return;
        }
        OutpatientChargeOrder update = new OutpatientChargeOrder();
        update.setId(orderId);
        update.setStatus(1);
        update.setChargeTime(LocalDateTime.now());
        update.setUpdateTime(LocalDateTime.now());
        outpatientChargeOrderMapper.updateById(update);
    }

    @Transactional
    public void payOutpatientChargeOrderByPatient(Long orderId, Long patientId) {
        OutpatientChargeOrder order = outpatientChargeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("缴费单不存在");
        }
        if (patientId == null || !patientId.equals(order.getPatientId())) {
            throw new ServiceException("无权支付该缴费单");
        }
        payOutpatientChargeOrder(orderId);
    }

    public Page<OutpatientChargeOrder> pageOutpatientChargeOrders(Long patientId, String patientName, Integer status, String orderNo,
                                                                  String creatorName,
                                                                  LocalDate createDateStart,
                                                                  LocalDate createDateEnd,
                                                                  Integer currentPage, Integer size) {
        List<Long> patientIdsFromName = listQuerySupport.resolvePatientIdsByKeyword(patientName);
        if (patientIdsFromName != null && patientIdsFromName.isEmpty()) {
            return new Page<>(currentPage, size, 0);
        }
        List<Long> creatorIds = listQuerySupport.resolveUserIdsByKeyword(creatorName);
        if (creatorIds != null && creatorIds.isEmpty()) {
            return new Page<>(currentPage, size, 0);
        }
        LambdaQueryWrapper<OutpatientChargeOrder> qw = new LambdaQueryWrapper<>();
        if (patientId != null) {
            qw.eq(OutpatientChargeOrder::getPatientId, patientId);
        }
        if (patientIdsFromName != null) {
            qw.in(OutpatientChargeOrder::getPatientId, patientIdsFromName);
        }
        if (creatorIds != null) {
            qw.in(OutpatientChargeOrder::getCashierUserId, creatorIds);
        }
        ListQuerySupport.applyCreateTimeDateRange(qw, OutpatientChargeOrder::getCreateTime, createDateStart, createDateEnd);
        if (status != null) {
            qw.eq(OutpatientChargeOrder::getStatus, status);
        }
        if (StringUtils.isNotBlank(orderNo)) {
            qw.like(OutpatientChargeOrder::getOrderNo, orderNo.trim());
        }
        qw.orderByDesc(OutpatientChargeOrder::getUpdateTime);
        Page<OutpatientChargeOrder> page = outpatientChargeOrderMapper.selectPage(new Page<>(currentPage, size), qw);
        fillOutpatientOrders(page.getRecords());
        return page;
    }

    public OutpatientChargeOrder getOutpatientChargeOrder(Long orderId) {
        OutpatientChargeOrder order = outpatientChargeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("缴费单不存在");
        }
        fillOutpatientOrders(List.of(order));
        return order;
    }

    @Transactional
    public int generatePurchaseSettlementDetails() {
        int created = 0;
        List<StockInOrder> postedStockInOrders = stockInOrderMapper.selectList(
                new LambdaQueryWrapper<StockInOrder>().eq(StockInOrder::getStatus, 1)
        );
        LocalDateTime now = LocalDateTime.now();
        for (StockInOrder stockInOrder : postedStockInOrders) {
            List<StockInOrderItem> items = stockInOrderItemMapper.selectList(
                    new LambdaQueryWrapper<StockInOrderItem>().eq(StockInOrderItem::getStockInId, stockInOrder.getId())
            );
            for (StockInOrderItem item : items) {
                PurchaseAcceptanceItem accItem = purchaseAcceptanceItemMapper.selectById(item.getAcceptanceItemId());
                if (accItem == null) {
                    continue;
                }
                PurchaseAcceptance acceptance = purchaseAcceptanceMapper.selectById(accItem.getAcceptanceId());
                if (acceptance == null) {
                    continue;
                }
                PurchaseOrder purchaseOrder = purchaseOrderMapper.selectById(acceptance.getPurchaseOrderId());
                if (purchaseOrder == null) {
                    continue;
                }
                Long supplierId = purchaseOrder.getSupplierId();
                Long exists = purchaseSettlementDetailMapper.selectCount(
                        new LambdaQueryWrapper<PurchaseSettlementDetail>()
                                .eq(PurchaseSettlementDetail::getStockInItemId, item.getId())
                );
                if (exists != null && exists > 0) {
                    continue;
                }

                PurchaseSettlementDetail detail = new PurchaseSettlementDetail();
                detail.setStockInId(stockInOrder.getId());
                detail.setStockInItemId(item.getId());
                detail.setSupplierId(supplierId);
                detail.setMedicineId(item.getMedicineId());
                detail.setQuantity(item.getStockInQty());
                detail.setUnitCost(item.getUnitCost() == null ? BigDecimal.ZERO : item.getUnitCost());
                detail.setAmount(item.getAmount() == null ? BigDecimal.ZERO : item.getAmount());
                detail.setStatus(0);
                detail.setCreateTime(now);
                detail.setUpdateTime(now);
                purchaseSettlementDetailMapper.insert(detail);
                created++;
            }
        }
        return created;
    }

    public Page<PurchaseSettlementDetail> pagePurchaseSettlementDetails(Long supplierId, Integer status,
                                                                        List<Long> stockInIds,
                                                                        Integer currentPage, Integer size) {
        LambdaQueryWrapper<PurchaseSettlementDetail> qw = new LambdaQueryWrapper<>();
        if (supplierId != null) {
            qw.eq(PurchaseSettlementDetail::getSupplierId, supplierId);
        }
        if (status != null) {
            qw.eq(PurchaseSettlementDetail::getStatus, status);
        }
        if (stockInIds != null && !stockInIds.isEmpty()) {
            qw.in(PurchaseSettlementDetail::getStockInId, stockInIds);
        }
        qw.orderByDesc(PurchaseSettlementDetail::getUpdateTime);
        Page<PurchaseSettlementDetail> page = purchaseSettlementDetailMapper.selectPage(new Page<>(currentPage, size), qw);
        fillSettlementDetails(page.getRecords());
        return page;
    }

    /**
     * 存在「未生成结算单」明细的供应商列表，供创建结算单前先选择供应商。
     */
    public List<PendingSettlementSupplierDTO> listPendingSettlementSuppliers() {
        List<PurchaseSettlementDetail> pendings = purchaseSettlementDetailMapper.selectList(
                new LambdaQueryWrapper<PurchaseSettlementDetail>().eq(PurchaseSettlementDetail::getStatus, 0)
        );
        Map<Long, Long> countBySupplier = pendings.stream()
                .filter(d -> d.getSupplierId() != null)
                .collect(Collectors.groupingBy(PurchaseSettlementDetail::getSupplierId, Collectors.counting()));
        List<Long> sortedIds = new ArrayList<>(countBySupplier.keySet());
        sortedIds.sort(Comparator.naturalOrder());
        List<PendingSettlementSupplierDTO> list = new ArrayList<>();
        for (Long sid : sortedIds) {
            Supplier supplier = supplierMapper.selectById(sid);
            PendingSettlementSupplierDTO dto = new PendingSettlementSupplierDTO();
            dto.setSupplierId(sid);
            dto.setSupplierName(supplier != null && StringUtils.isNotBlank(supplier.getName()) ? supplier.getName() : String.valueOf(sid));
            dto.setPendingLineCount(countBySupplier.get(sid).intValue());
            list.add(dto);
        }
        return list;
    }

    /**
     * 存在「未生成结算单」明细的入库单列表；若传入 supplierId 则仅返回该供应商的入库单。
     */
    public List<PendingSettlementStockInDTO> listPendingSettlementStockIns(Long supplierIdFilter) {
        List<PurchaseSettlementDetail> pendings = purchaseSettlementDetailMapper.selectList(
                new LambdaQueryWrapper<PurchaseSettlementDetail>().eq(PurchaseSettlementDetail::getStatus, 0)
        );
        if (supplierIdFilter != null) {
            pendings = pendings.stream()
                    .filter(d -> supplierIdFilter.equals(d.getSupplierId()))
                    .collect(Collectors.toList());
        }
        Map<Long, Long> countByStockIn = pendings.stream()
                .filter(d -> d.getStockInId() != null)
                .collect(Collectors.groupingBy(PurchaseSettlementDetail::getStockInId, Collectors.counting()));
        List<Long> sortedIds = new ArrayList<>(countByStockIn.keySet());
        sortedIds.sort(Comparator.naturalOrder());
        List<PendingSettlementStockInDTO> list = new ArrayList<>();
        for (Long stockInId : sortedIds) {
            StockInOrder stockIn = stockInOrderMapper.selectById(stockInId);
            PurchaseSettlementDetail sample = pendings.stream()
                    .filter(d -> stockInId.equals(d.getStockInId()))
                    .findFirst()
                    .orElse(null);
            Supplier supplier = sample != null && sample.getSupplierId() != null
                    ? supplierMapper.selectById(sample.getSupplierId()) : null;
            PendingSettlementStockInDTO dto = new PendingSettlementStockInDTO();
            dto.setStockInId(stockInId);
            dto.setStockInNo(stockIn != null && StringUtils.isNotBlank(stockIn.getStockInNo()) ? stockIn.getStockInNo() : String.valueOf(stockInId));
            dto.setSupplierName(supplier != null && StringUtils.isNotBlank(supplier.getName()) ? supplier.getName() : "-");
            dto.setPendingLineCount(countByStockIn.get(stockInId).intValue());
            list.add(dto);
        }
        return list;
    }

    @Transactional
    public PurchaseSettlementOrder createPurchaseSettlementOrder(List<Long> detailIds, String remark) {
        if (detailIds == null || detailIds.isEmpty()) {
            throw new ServiceException("请选择要结算的明细");
        }
        List<PurchaseSettlementDetail> details = purchaseSettlementDetailMapper.selectBatchIds(detailIds);
        if (details.size() != detailIds.size()) {
            throw new ServiceException("存在无效的结算明细");
        }
        Long supplierId = null;
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseSettlementDetail detail : details) {
            if (detail.getStatus() != null && detail.getStatus() == 1) {
                throw new ServiceException("选中的结算明细存在已生成单据记录");
            }
            if (supplierId == null) {
                supplierId = detail.getSupplierId();
            } else if (!supplierId.equals(detail.getSupplierId())) {
                throw new ServiceException("一张结算单仅允许同一供应商的结算明细");
            }
            total = total.add(detail.getAmount() == null ? BigDecimal.ZERO : detail.getAmount());
        }

        LocalDateTime now = LocalDateTime.now();
        PurchaseSettlementOrder order = new PurchaseSettlementOrder();
        order.setSettlementNo(generateSettlementNo());
        order.setSupplierId(supplierId);
        order.setTotalAmount(total);
        order.setStatus(0);
        order.setRemark(remark);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        try {
            order.setCashierUserId(JwtTokenUtils.getCurrentUserId());
        } catch (Exception ignored) {
        }
        purchaseSettlementOrderMapper.insert(order);

        for (PurchaseSettlementDetail detail : details) {
            PurchaseSettlementDetail update = new PurchaseSettlementDetail();
            update.setId(detail.getId());
            update.setStatus(1);
            update.setSettlementOrderId(order.getId());
            update.setUpdateTime(now);
            purchaseSettlementDetailMapper.updateById(update);
        }
        return getPurchaseSettlementOrder(order.getId());
    }

    @Transactional
    public void settlePurchaseSettlementOrder(Long orderId) {
        PurchaseSettlementOrder order = purchaseSettlementOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("结算单不存在");
        }
        if (order.getStatus() != null && order.getStatus() == 1) {
            return;
        }
        PurchaseSettlementOrder update = new PurchaseSettlementOrder();
        update.setId(orderId);
        update.setStatus(1);
        update.setSettlementTime(LocalDateTime.now());
        update.setUpdateTime(LocalDateTime.now());
        purchaseSettlementOrderMapper.updateById(update);
    }

    public Page<PurchaseSettlementOrder> pagePurchaseSettlementOrders(Long supplierId, String supplierName, Integer status, String settlementNo,
                                                                       String creatorName,
                                                                       LocalDate createDateStart,
                                                                       LocalDate createDateEnd,
                                                                       Integer currentPage, Integer size) {
        List<Long> supplierIdsFromName = listQuerySupport.resolveSupplierIdsByName(supplierName);
        if (supplierIdsFromName != null && supplierIdsFromName.isEmpty()) {
            return new Page<>(currentPage, size, 0);
        }
        List<Long> creatorIds = listQuerySupport.resolveUserIdsByKeyword(creatorName);
        if (creatorIds != null && creatorIds.isEmpty()) {
            return new Page<>(currentPage, size, 0);
        }
        LambdaQueryWrapper<PurchaseSettlementOrder> qw = new LambdaQueryWrapper<>();
        if (supplierId != null) {
            qw.eq(PurchaseSettlementOrder::getSupplierId, supplierId);
        }
        if (supplierIdsFromName != null) {
            qw.in(PurchaseSettlementOrder::getSupplierId, supplierIdsFromName);
        }
        if (creatorIds != null) {
            qw.in(PurchaseSettlementOrder::getCashierUserId, creatorIds);
        }
        ListQuerySupport.applyCreateTimeDateRange(qw, PurchaseSettlementOrder::getCreateTime, createDateStart, createDateEnd);
        if (status != null) {
            qw.eq(PurchaseSettlementOrder::getStatus, status);
        }
        if (StringUtils.isNotBlank(settlementNo)) {
            qw.like(PurchaseSettlementOrder::getSettlementNo, settlementNo.trim());
        }
        qw.orderByDesc(PurchaseSettlementOrder::getUpdateTime);
        Page<PurchaseSettlementOrder> page = purchaseSettlementOrderMapper.selectPage(new Page<>(currentPage, size), qw);
        fillSettlementOrders(page.getRecords());
        return page;
    }

    public PurchaseSettlementOrder getPurchaseSettlementOrder(Long orderId) {
        PurchaseSettlementOrder order = purchaseSettlementOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("结算单不存在");
        }
        fillSettlementOrders(List.of(order));
        return order;
    }

    private void fillOutpatientDetails(List<OutpatientChargeDetail> records) {
        for (OutpatientChargeDetail detail : records) {
            detail.setPatient(patientMapper.selectById(detail.getPatientId()));
            detail.setMedicine(medicineMapper.selectById(detail.getMedicineId()));
            detail.setPrescription(prescriptionMapper.selectById(detail.getPrescriptionId()));
        }
    }

    private void fillOutpatientOrders(List<OutpatientChargeOrder> records) {
        for (OutpatientChargeOrder order : records) {
            order.setPatient(patientMapper.selectById(order.getPatientId()));
            List<OutpatientChargeDetail> details = outpatientChargeDetailMapper.selectList(
                    new LambdaQueryWrapper<OutpatientChargeDetail>().eq(OutpatientChargeDetail::getChargeOrderId, order.getId())
            );
            fillOutpatientDetails(details);
            order.setDetails(details);
            List<Long> rxIds = details.stream()
                    .map(OutpatientChargeDetail::getPrescriptionId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted(Comparator.naturalOrder())
                    .toList();
            order.setPrescriptionIds(rxIds);
        }
    }

    private void fillSettlementDetails(List<PurchaseSettlementDetail> records) {
        for (PurchaseSettlementDetail detail : records) {
            detail.setSupplier(supplierMapper.selectById(detail.getSupplierId()));
            detail.setMedicine(medicineMapper.selectById(detail.getMedicineId()));
            detail.setStockInOrder(stockInOrderMapper.selectById(detail.getStockInId()));
        }
    }

    private void fillSettlementOrders(List<PurchaseSettlementOrder> records) {
        for (PurchaseSettlementOrder order : records) {
            order.setSupplier(supplierMapper.selectById(order.getSupplierId()));
            List<PurchaseSettlementDetail> details = purchaseSettlementDetailMapper.selectList(
                    new LambdaQueryWrapper<PurchaseSettlementDetail>().eq(PurchaseSettlementDetail::getSettlementOrderId, order.getId())
            );
            fillSettlementDetails(details);
            order.setDetails(details);
            List<Long> siIds = details.stream()
                    .map(PurchaseSettlementDetail::getStockInId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted(Comparator.naturalOrder())
                    .toList();
            order.setStockInIds(siIds);
        }
    }

    private String generateOutpatientOrderNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = String.format("%06d", (int) (Math.random() * 1000000));
        return "OC" + dateStr + randomStr;
    }

    private String generateSettlementNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = String.format("%06d", (int) (Math.random() * 1000000));
        return "PS" + dateStr + randomStr;
    }
}
