package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.example.springboot.entity.*;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PrescriptionService {
    @Resource
    private PrescriptionMapper prescriptionMapper;
    
    @Resource
    private PrescriptionDetailMapper prescriptionDetailMapper;
    
    @Resource
    private MedicalRecordMapper medicalRecordMapper;
    
    @Resource
    private PatientMapper patientMapper;
    
    @Resource
    private DoctorMapper doctorMapper;
    
    @Resource
    private MedicineMapper medicineMapper;
    @Resource
    private MedicalRecordDetailMapper medicalRecordDetailMapper;

    @Resource
    private AppointmentMapper appointmentMapper;
    
    /**
     * 创建处方
     */
    @Transactional
    public Prescription createPrescription(Prescription prescription) {
        // 检查就诊记录是否存在
        MedicalRecord medicalRecord = medicalRecordMapper.selectById(prescription.getRecordId());
        if (medicalRecord == null) {
            throw new ServiceException("就诊记录不存在");
        }
        if (!medicalRecord.getPatientId().equals(prescription.getPatientId())) {
            throw new ServiceException("处方患者与就诊记录患者不一致");
        }
        if (!medicalRecord.getDoctorId().equals(prescription.getDoctorId())) {
            throw new ServiceException("处方医生与就诊记录医生不一致");
        }
        // 诊断结果始终与病历保持同步
        prescription.setDiagnosis(medicalRecord.getDiagnosis());
        if (medicalRecord.getAppointmentId() != null) {
            Appointment appointment = appointmentMapper.selectById(medicalRecord.getAppointmentId());
            if (appointment == null) {
                throw new ServiceException("病历关联预约不存在");
            }
            if (appointment.getStatus() == null || appointment.getStatus() != 2) {
                throw new ServiceException("未确认就诊，不能进入病历处方流程");
            }
        }
        
        // 检查患者是否存在
        Patient patient = patientMapper.selectById(prescription.getPatientId());
        if (patient == null) {
            throw new ServiceException("患者不存在");
        }
        
        // 检查医生是否存在
        Doctor doctor = doctorMapper.selectById(prescription.getDoctorId());
        if (doctor == null) {
            throw new ServiceException("医生不存在");
        }
        
        // 生成处方编号
        prescription.setPrescriptionNo(generatePrescriptionNo());
        
        // 如果没有指定处方日期，使用当前日期
        if (prescription.getPrescriptionDate() == null) {
            prescription.setPrescriptionDate(LocalDate.now());
        }
        
        // 新建处方均为待提交，由医生提交后进入待取药
        prescription.setStatus(0);
        
        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        prescription.setCreateTime(now);
        prescription.setUpdateTime(now);
        
        if (prescriptionMapper.insert(prescription) <= 0) {
            throw new ServiceException("处方创建失败");
        }
        
        // 处理处方明细
        if (prescription.getDetails() != null && !prescription.getDetails().isEmpty()) {
            for (PrescriptionDetail detail : prescription.getDetails()) {
                // 检查药品是否存在
                Medicine medicine = medicineMapper.selectById(detail.getMedicineId());
                if (medicine == null) {
                    throw new ServiceException("药品不存在: ID=" + detail.getMedicineId());
                }
                if (detail.getMedicalRecordDetailId() != null) {
                    MedicalRecordDetail medicalRecordDetail = medicalRecordDetailMapper.selectById(detail.getMedicalRecordDetailId());
                    if (medicalRecordDetail == null) {
                        throw new ServiceException("病症明细不存在: ID=" + detail.getMedicalRecordDetailId());
                    }
                    if (!prescription.getRecordId().equals(medicalRecordDetail.getRecordId())) {
                        throw new ServiceException("处方明细病症不属于当前就诊记录");
                    }
                }
                
                detail.setPrescriptionId(prescription.getId());
                detail.setCreateTime(now);
                detail.setUpdateTime(now);
                
                if (prescriptionDetailMapper.insert(detail) <= 0) {
                    throw new ServiceException("处方明细创建失败");
                }
            }
        }
        
        return prescription;
    }
    
    /**
     * 医生提交处方，进入待取药（同步到药房取药列表）
     */
    @Transactional
    public void submitPrescription(Long id) {
        Prescription prescription = prescriptionMapper.selectById(id);
        if (prescription == null) {
            throw new ServiceException("处方不存在");
        }
        if (prescription.getStatus() == null || prescription.getStatus() != 0) {
            throw new ServiceException("仅待提交处方可提交");
        }
        LambdaQueryWrapper<PrescriptionDetail> dw = new LambdaQueryWrapper<>();
        dw.eq(PrescriptionDetail::getPrescriptionId, id);
        long detailCount = prescriptionDetailMapper.selectCount(dw);
        if (detailCount <= 0) {
            throw new ServiceException("请至少保留一条处方明细后再提交");
        }
        Prescription update = new Prescription();
        update.setId(id);
        update.setStatus(1);
        update.setUpdateTime(LocalDateTime.now());
        if (prescriptionMapper.updateById(update) <= 0) {
            throw new ServiceException("处方提交失败");
        }
    }

    /**
     * 药房标记已取药：待取药 -> 已取药
     */
    @Transactional
    public void updatePrescriptionStatus(Long id, Integer status) {
        Prescription prescription = prescriptionMapper.selectById(id);
        if (prescription == null) {
            throw new ServiceException("处方不存在");
        }
        if (status == null || status != 2) {
            throw new ServiceException("仅支持标记为已取药");
        }
        if (prescription.getStatus() == null || prescription.getStatus() != 1) {
            throw new ServiceException("仅待取药处方可标记已取药");
        }
        
        Prescription updatePrescription = new Prescription();
        updatePrescription.setId(id);
        updatePrescription.setStatus(2);
        updatePrescription.setUpdateTime(LocalDateTime.now());
        
        if (prescriptionMapper.updateById(updatePrescription) <= 0) {
            throw new ServiceException("处方状态更新失败");
        }
    }
    
    /**
     * 获取处方详情
     */
    public Prescription getPrescriptionById(Long id) {
        Prescription prescription = prescriptionMapper.selectById(id);
        if (prescription == null) {
            throw new ServiceException("处方不存在");
        }
        
        // 查询患者信息
        Patient patient = patientMapper.selectById(prescription.getPatientId());
        prescription.setPatient(patient);
        
        // 查询医生信息
        Doctor doctor = doctorMapper.selectById(prescription.getDoctorId());
        prescription.setDoctor(doctor);
        
        // 查询就诊记录信息
        MedicalRecord medicalRecord = medicalRecordMapper.selectById(prescription.getRecordId());
        if (medicalRecord != null && medicalRecord.getAppointmentId() != null) {
            Appointment appointment = appointmentMapper.selectById(medicalRecord.getAppointmentId());
            medicalRecord.setAppointment(appointment);
        }
        prescription.setMedicalRecord(medicalRecord);
        
        // 查询处方明细
        LambdaQueryWrapper<PrescriptionDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PrescriptionDetail::getPrescriptionId, id);
        List<PrescriptionDetail> details = prescriptionDetailMapper.selectList(queryWrapper);
        
        // 查询药品信息
        for (PrescriptionDetail detail : details) {
            Medicine medicine = medicineMapper.selectById(detail.getMedicineId());
            detail.setMedicine(medicine);
            if (detail.getMedicalRecordDetailId() != null) {
                detail.setMedicalRecordDetail(medicalRecordDetailMapper.selectById(detail.getMedicalRecordDetailId()));
            }
        }
        
        prescription.setDetails(details);
        fillPricing(prescription);
        
        return prescription;
    }
    
    /**
     * 分页查询处方列表
     */
    public Page<Prescription> getPrescriptionsByPage(String prescriptionNo, Long patientId, Long doctorId, 
                                               String patientName, String doctorName, Long recordId, 
                                               LocalDate startDate, LocalDate endDate, Integer status, 
                                               Integer currentPage, Integer size) {
        Page<Prescription> page = new Page<>(currentPage, size);
        
        LambdaQueryWrapper<Prescription> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加查询条件
        if (StringUtils.isNotBlank(prescriptionNo)) {
            queryWrapper.like(Prescription::getPrescriptionNo, prescriptionNo);
        }
        
        if (patientId != null) {
            queryWrapper.eq(Prescription::getPatientId, patientId);
        }
        
        if (doctorId != null) {
            queryWrapper.eq(Prescription::getDoctorId, doctorId);
        }
        
        if (recordId != null) {
            queryWrapper.eq(Prescription::getRecordId, recordId);
        }
        
        if (startDate != null) {
            queryWrapper.ge(Prescription::getPrescriptionDate, startDate);
        }
        
        if (endDate != null) {
            queryWrapper.le(Prescription::getPrescriptionDate, endDate);
        }
        
        if (status != null) {
            queryWrapper.eq(Prescription::getStatus, status);
        }
        
        // 按创建时间降序排序
        queryWrapper.orderByDesc(Prescription::getCreateTime);
        
        // 如果有患者姓名或医生姓名查询条件，需要进行关联查询
        if (StringUtils.isNotBlank(patientName) || StringUtils.isNotBlank(doctorName)) {
            // 使用自定义SQL查询
            return prescriptionMapper.selectPrescriptionsByNamePage(
                page, prescriptionNo, patientId, doctorId, patientName, doctorName, 
                recordId, startDate, endDate, status);
        }
        
        Page<Prescription> resultPage = prescriptionMapper.selectPage(page, queryWrapper);
            
        // 填充关联信息
        fillPrescriptionsInfo(resultPage.getRecords());
        
        return resultPage;
    }
    
    /**
     * 获取患者处方列表
     */
    public List<Prescription> getPrescriptionsByPatient(Long patientId) {
        LambdaQueryWrapper<Prescription> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Prescription::getPatientId, patientId)
                   .orderByDesc(Prescription::getPrescriptionDate);
        
        List<Prescription> prescriptions = prescriptionMapper.selectList(queryWrapper);
        
        // 查询关联信息
        for (Prescription prescription : prescriptions) {
            // 查询医生信息
            Doctor doctor = doctorMapper.selectById(prescription.getDoctorId());
            prescription.setDoctor(doctor);
        }
        
        return prescriptions;
    }
    
    /**
     * 获取就诊记录关联的处方列表
     */
    public List<Prescription> getPrescriptionsByRecord(Long recordId) {
        LambdaQueryWrapper<Prescription> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Prescription::getRecordId, recordId)
                   .orderByDesc(Prescription::getPrescriptionDate);
        
        List<Prescription> prescriptions = prescriptionMapper.selectList(queryWrapper);
        
        // 查询关联信息
        for (Prescription prescription : prescriptions) {
            // 查询患者信息
            Patient patient = patientMapper.selectById(prescription.getPatientId());
            prescription.setPatient(patient);
            
            // 查询医生信息
            Doctor doctor = doctorMapper.selectById(prescription.getDoctorId());
            prescription.setDoctor(doctor);
            
            // 查询处方明细
            LambdaQueryWrapper<PrescriptionDetail> detailQueryWrapper = new LambdaQueryWrapper<>();
            detailQueryWrapper.eq(PrescriptionDetail::getPrescriptionId, prescription.getId());
            List<PrescriptionDetail> details = prescriptionDetailMapper.selectList(detailQueryWrapper);
            
            // 查询药品信息
            for (PrescriptionDetail detail : details) {
                Medicine medicine = medicineMapper.selectById(detail.getMedicineId());
                detail.setMedicine(medicine);
                if (detail.getMedicalRecordDetailId() != null) {
                    detail.setMedicalRecordDetail(medicalRecordDetailMapper.selectById(detail.getMedicalRecordDetailId()));
                }
            }
            
            prescription.setDetails(details);
            fillPricing(prescription);

            MedicalRecord medicalRecord = medicalRecordMapper.selectById(prescription.getRecordId());
            if (medicalRecord != null && medicalRecord.getAppointmentId() != null) {
                Appointment appointment = appointmentMapper.selectById(medicalRecord.getAppointmentId());
                medicalRecord.setAppointment(appointment);
            }
            prescription.setMedicalRecord(medicalRecord);
        }
        
        return prescriptions;
    }
    
    /**
     * 添加处方明细
     */
    @Transactional
    public PrescriptionDetail addPrescriptionDetail(PrescriptionDetail detail) {
        // 检查处方是否存在
        Prescription prescription = prescriptionMapper.selectById(detail.getPrescriptionId());
        if (prescription == null) {
            throw new ServiceException("处方不存在");
        }
        if (prescription.getStatus() != null && prescription.getStatus() != 0) {
            throw new ServiceException("仅待提交处方可添加明细");
        }
        
        // 检查药品是否存在
        Medicine medicine = medicineMapper.selectById(detail.getMedicineId());
        if (medicine == null) {
            throw new ServiceException("药品不存在");
        }
        if (detail.getMedicalRecordDetailId() != null) {
            MedicalRecordDetail medicalRecordDetail = medicalRecordDetailMapper.selectById(detail.getMedicalRecordDetailId());
            if (medicalRecordDetail == null) {
                throw new ServiceException("病症明细不存在");
            }
            if (!prescription.getRecordId().equals(medicalRecordDetail.getRecordId())) {
                throw new ServiceException("病症明细不属于该处方就诊记录");
            }
        }
        
        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        detail.setCreateTime(now);
        detail.setUpdateTime(now);
        
        if (prescriptionDetailMapper.insert(detail) <= 0) {
            throw new ServiceException("处方明细添加失败");
        }
        
        return detail;
    }
    
    /**
     * 更新处方明细
     */
    @Transactional
    public void updatePrescriptionDetail(Long id, PrescriptionDetail detail) {
        // 检查处方明细是否存在
        PrescriptionDetail existingDetail = prescriptionDetailMapper.selectById(id);
        if (existingDetail == null) {
            throw new ServiceException("处方明细不存在");
        }
        Prescription prescription = prescriptionMapper.selectById(existingDetail.getPrescriptionId());
        if (prescription != null && prescription.getStatus() != null && prescription.getStatus() != 0) {
            throw new ServiceException("仅待提交处方可修改明细");
        }
        
        detail.setId(id);
        detail.setUpdateTime(LocalDateTime.now());
        
        if (prescriptionDetailMapper.updateById(detail) <= 0) {
            throw new ServiceException("处方明细更新失败");
        }
    }
    
    /**
     * 删除处方明细
     */
    @Transactional
    public void deletePrescriptionDetail(Long id) {
        PrescriptionDetail existingDetail = prescriptionDetailMapper.selectById(id);
        if (existingDetail == null) {
            throw new ServiceException("处方明细不存在");
        }
        Prescription prescription = prescriptionMapper.selectById(existingDetail.getPrescriptionId());
        if (prescription != null && prescription.getStatus() != null && prescription.getStatus() != 0) {
            throw new ServiceException("仅待提交处方可删除明细");
        }
        if (prescriptionDetailMapper.deleteById(id) <= 0) {
            throw new ServiceException("处方明细删除失败");
        }
    }
    
    /**
     * 生成处方编号
     */
    private String generatePrescriptionNo() {
        // 生成格式：P + 年月日 + 6位随机数
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = String.format("%06d", (int)(Math.random() * 1000000));
        return "P" + dateStr + randomStr;
    }
    
    /**
     * 填充处方关联信息
     */
    private void fillPrescriptionsInfo(List<Prescription> prescriptions) {
        if (prescriptions == null || prescriptions.isEmpty()) {
            return;
        }
        
        for (Prescription prescription : prescriptions) {
            // 查询患者信息
            Patient patient = patientMapper.selectById(prescription.getPatientId());
            prescription.setPatient(patient);
            
            // 查询医生信息
            Doctor doctor = doctorMapper.selectById(prescription.getDoctorId());
            prescription.setDoctor(doctor);
            
            // 查询就诊记录信息
            MedicalRecord medicalRecord = medicalRecordMapper.selectById(prescription.getRecordId());
            if (medicalRecord != null && medicalRecord.getAppointmentId() != null) {
                Appointment appointment = appointmentMapper.selectById(medicalRecord.getAppointmentId());
                medicalRecord.setAppointment(appointment);
            }
            prescription.setMedicalRecord(medicalRecord);
            
            // 查询处方明细
            LambdaQueryWrapper<PrescriptionDetail> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(PrescriptionDetail::getPrescriptionId, prescription.getId());
            List<PrescriptionDetail> details = prescriptionDetailMapper.selectList(queryWrapper);
            
            // 填充药品信息
            for (PrescriptionDetail detail : details) {
                Medicine medicine = medicineMapper.selectById(detail.getMedicineId());
                detail.setMedicine(medicine);
                if (detail.getMedicalRecordDetailId() != null) {
                    detail.setMedicalRecordDetail(medicalRecordDetailMapper.selectById(detail.getMedicalRecordDetailId()));
                }
            }
            
            prescription.setDetails(details);
            fillPricing(prescription);
        }
    }

    private void fillPricing(Prescription prescription) {
        if (prescription == null || prescription.getDetails() == null || prescription.getDetails().isEmpty()) {
            if (prescription != null) {
                prescription.setTotalAmount(BigDecimal.ZERO);
            }
            return;
        }
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (PrescriptionDetail detail : prescription.getDetails()) {
            Medicine medicine = medicineMapper.selectById(detail.getMedicineId());
            if (medicine != null) {
                detail.setMedicine(medicine);
            }
            BigDecimal salePrice = medicine == null || medicine.getSalePrice() == null
                    ? BigDecimal.ZERO
                    : medicine.getSalePrice();
            int quantity = detail.getQuantity() == null ? 0 : detail.getQuantity();
            BigDecimal amount = salePrice.multiply(BigDecimal.valueOf(quantity));
            detail.setSalePrice(salePrice);
            detail.setAmount(amount);
            totalAmount = totalAmount.add(amount);
        }
        prescription.setTotalAmount(totalAmount);
    }
} 