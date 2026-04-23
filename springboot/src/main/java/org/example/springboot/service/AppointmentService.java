package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.example.springboot.entity.Appointment;
import org.example.springboot.entity.Doctor;
import org.example.springboot.entity.Patient;
import org.example.springboot.entity.Department;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.AppointmentMapper;
import org.example.springboot.mapper.DoctorMapper;
import org.example.springboot.mapper.PatientMapper;
import org.example.springboot.mapper.DepartmentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AppointmentService {
    @Resource
    private AppointmentMapper appointmentMapper;
    
    @Resource
    private PatientMapper patientMapper;
    
    @Resource
    private DoctorMapper doctorMapper;
    
    @Resource
    private DepartmentMapper departmentMapper;
    
    /**
     * 创建预约
     */
    @Transactional
    public Appointment createAppointment(Appointment appointment) {
        // 检查患者是否存在
        Patient patient = patientMapper.selectById(appointment.getPatientId());
        if (patient == null) {
            throw new ServiceException("患者不存在");
        }
        
        // 检查医生是否存在
        Doctor doctor = doctorMapper.selectById(appointment.getDoctorId());
        if (doctor == null) {
            throw new ServiceException("医生不存在");
        }
        
        if (appointment.getAppointmentDate() == null) {
            throw new ServiceException("预约日期不能为空");
        }
        if (appointment.getAppointmentDate().isBefore(LocalDate.now())) {
            throw new ServiceException("预约日期不能早于今天");
        }
        if (StringUtils.isBlank(appointment.getTimeSlot())) {
            throw new ServiceException("预约时间段不能为空");
        }
        if (!isValidTimeSlot(appointment.getTimeSlot())) {
            throw new ServiceException("预约时间段不合法，仅支持上午/下午/晚上");
        }
        
        // 检查患者是否已经预约过同一天同一医生的门诊
        LambdaQueryWrapper<Appointment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Appointment::getPatientId, appointment.getPatientId())
                   .eq(Appointment::getDoctorId, appointment.getDoctorId())
                   .eq(Appointment::getAppointmentDate, appointment.getAppointmentDate())
                   .eq(Appointment::getStatus, 1); // 待就诊状态
        
        if (appointmentMapper.selectCount(queryWrapper) > 0) {
            throw new ServiceException("您已预约过该医生当天的门诊");
        }
        
        // 生成预约编号
        appointment.setAppointmentNo(generateAppointmentNo());
        
        // 兼容旧库结构：appointment.schedule_id 仍为非空字段
        if (appointment.getScheduleId() == null) {
            appointment.setScheduleId(0L);
        }
        
        // 设置状态为待就诊
        appointment.setStatus(1);
        
        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        appointment.setCreateTime(now);
        appointment.setUpdateTime(now);
        
        // 保存预约信息
        if (appointmentMapper.insert(appointment) <= 0) {
            throw new ServiceException("预约失败");
        }
        
        return appointment;
    }
    
    /**
     * 取消预约
     */
    @Transactional
    public void cancelAppointment(Long id) {
        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment == null) {
            throw new ServiceException("预约不存在");
        }
        
        // 检查预约状态是否为待就诊
        if (appointment.getStatus() != 1) {
            throw new ServiceException("只能取消待就诊的预约");
        }
        
        // 检查预约日期是否已过
        if (appointment.getAppointmentDate().isBefore(LocalDate.now())) {
            throw new ServiceException("无法取消过期的预约");
        }
        
        // 更新预约状态为已取消
        Appointment updateAppointment = new Appointment();
        updateAppointment.setId(id);
        updateAppointment.setStatus(0);
        updateAppointment.setUpdateTime(LocalDateTime.now());
        
        if (appointmentMapper.updateById(updateAppointment) <= 0) {
            throw new ServiceException("取消预约失败");
        }
        
    }
    
    /**
     * 完成就诊
     */
    @Transactional
    public void completeAppointment(Long id) {
        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment == null) {
            throw new ServiceException("预约不存在");
        }
        
        // 检查预约状态是否为待就诊
        if (appointment.getStatus() != 1) {
            throw new ServiceException("只能完成待就诊的预约");
        }
        
        // 更新预约状态为已就诊
        Appointment updateAppointment = new Appointment();
        updateAppointment.setId(id);
        updateAppointment.setStatus(2);
        updateAppointment.setUpdateTime(LocalDateTime.now());
        
        if (appointmentMapper.updateById(updateAppointment) <= 0) {
            throw new ServiceException("完成就诊失败");
        }
    }

    /**
     * 门诊完成
     */
    @Transactional
    public void finishAppointment(Long id) {
        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment == null) {
            throw new ServiceException("预约不存在");
        }

        // 仅允许将已就诊记录标记为已完成就诊
        if (appointment.getStatus() == null || appointment.getStatus() != 2) {
            throw new ServiceException("只能完成已就诊的预约");
        }

        Appointment updateAppointment = new Appointment();
        updateAppointment.setId(id);
        updateAppointment.setStatus(3);
        updateAppointment.setUpdateTime(LocalDateTime.now());

        if (appointmentMapper.updateById(updateAppointment) <= 0) {
            throw new ServiceException("完成就诊失败");
        }
    }
    
    /**
     * 获取预约详情
     */
    public Appointment getAppointmentById(Long id) {
        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment == null) {
            throw new ServiceException("预约不存在");
        }
        
        // 查询患者信息
        Patient patient = patientMapper.selectById(appointment.getPatientId());
        appointment.setPatient(patient);
        
        // 查询医生信息
        Doctor doctor = doctorMapper.selectById(appointment.getDoctorId());
        if (doctor != null) {
            // 查询科室信息
            if (doctor.getDepartmentId() != null) {
                Department department = departmentMapper.selectById(doctor.getDepartmentId());
                doctor.setDepartment(department);
            }
            appointment.setDoctor(doctor);
        }
        
        return appointment;
    }
    
    /**
     * 分页查询预约列表
     */
    public Page<Appointment> getAppointmentsByPage(Long patientId, Long doctorId, String patientName, String doctorName,
                                               LocalDate startDate, LocalDate endDate, Integer status, 
                                               Integer currentPage, Integer size) {
        Page<Appointment> page = new Page<>(currentPage, size);
        
        LambdaQueryWrapper<Appointment> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加查询条件
        if (patientId != null) {
            queryWrapper.eq(Appointment::getPatientId, patientId);
        }
        
        if (doctorId != null) {
            queryWrapper.eq(Appointment::getDoctorId, doctorId);
        }
        
        if (startDate != null) {
            queryWrapper.ge(Appointment::getAppointmentDate, startDate);
        }
        
        if (endDate != null) {
            queryWrapper.le(Appointment::getAppointmentDate, endDate);
        }
        
        if (status != null) {
            queryWrapper.eq(Appointment::getStatus, status);
        }
        if(StringUtils.isNotBlank(patientName)){
            List<Long> ids = patientMapper.selectList(new LambdaQueryWrapper<Patient>().like(Patient::getName, patientName)).stream().map(Patient::getId).toList();
            if(!ids.isEmpty()){
                queryWrapper.in(Appointment::getPatientId, ids);
            }else{
                return new Page<>(currentPage, size);
            }

        }
        if(StringUtils.isNotBlank(doctorName)){
            List<Long> ids = doctorMapper.selectList(new LambdaQueryWrapper<Doctor>().like(Doctor::getName, doctorName)).stream().map(Doctor::getId).toList();
            if(!ids.isEmpty()){
                queryWrapper.in(Appointment::getDoctorId, ids);
            }else{
                return new Page<>(currentPage, size);
            }

        }
        
        // 按创建时间降序排序
        queryWrapper.orderByDesc(Appointment::getCreateTime);

        
        Page<Appointment> resultPage = appointmentMapper.selectPage(page, queryWrapper);
        
        // 填充患者和医生信息
        fillAppointmentsInfo(resultPage.getRecords());
        
        return resultPage;
    }
    
    /**
     * 获取患者预约列表
     */
    public List<Appointment> getAppointmentsByPatient(Long patientId) {
        LambdaQueryWrapper<Appointment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Appointment::getPatientId, patientId)
                   .orderByDesc(Appointment::getAppointmentDate)
                   .orderByAsc(Appointment::getTimeSlot);
        
        List<Appointment> appointments = appointmentMapper.selectList(queryWrapper);
        
        // 查询关联信息
        for (Appointment appointment : appointments) {
            // 查询医生信息
            Doctor doctor = doctorMapper.selectById(appointment.getDoctorId());
            if (doctor != null) {
                // 查询科室信息
                if (doctor.getDepartmentId() != null) {
                    Department department = departmentMapper.selectById(doctor.getDepartmentId());
                    doctor.setDepartment(department);
                }
                appointment.setDoctor(doctor);
            }
            
        }
        
        return appointments;
    }
    
    /**
     * 获取医生预约列表
     */
    public List<Appointment> getAppointmentsByDoctor(Long doctorId, LocalDate date) {
        LambdaQueryWrapper<Appointment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Appointment::getDoctorId, doctorId);
        
        if (date != null) {
            queryWrapper.eq(Appointment::getAppointmentDate, date);
        }
        
        queryWrapper.orderByAsc(Appointment::getAppointmentDate)
                   .orderByAsc(Appointment::getTimeSlot);
        
        List<Appointment> appointments = appointmentMapper.selectList(queryWrapper);
        
        // 查询关联信息
        for (Appointment appointment : appointments) {
            // 查询患者信息
            Patient patient = patientMapper.selectById(appointment.getPatientId());
            appointment.setPatient(patient);
            
        }
        
        return appointments;
    }
    
    /**
     * 更新预约信息
     */
    @Transactional
    public void updateAppointment(Long id, Appointment appointment) {
        Appointment existingAppointment = appointmentMapper.selectById(id);
        if (existingAppointment == null) {
            throw new ServiceException("预约不存在");
        }
        
        // 检查预约状态是否为待就诊
        if (existingAppointment.getStatus() != 1) {
            throw new ServiceException("只能修改待就诊的预约");
        }
        
        // 检查预约日期是否已过
        if (existingAppointment.getAppointmentDate().isBefore(LocalDate.now())) {
            throw new ServiceException("无法修改过期的预约");
        }
        
        LocalDate targetDate = appointment.getAppointmentDate() == null
                ? existingAppointment.getAppointmentDate()
                : appointment.getAppointmentDate();
        if (targetDate != null && targetDate.isBefore(LocalDate.now())) {
            throw new ServiceException("预约日期不能早于今天");
        }
        String targetTimeSlot = StringUtils.isBlank(appointment.getTimeSlot())
                ? existingAppointment.getTimeSlot()
                : appointment.getTimeSlot();
        if (!isValidTimeSlot(targetTimeSlot)) {
            throw new ServiceException("预约时间段不合法，仅支持上午/下午/晚上");
        }
        Long targetDoctorId = appointment.getDoctorId() == null ? existingAppointment.getDoctorId() : appointment.getDoctorId();
        if (appointment.getDoctorId() != null && !appointment.getDoctorId().equals(existingAppointment.getDoctorId())) {
            Doctor doctor = doctorMapper.selectById(appointment.getDoctorId());
            if (doctor == null) {
                throw new ServiceException("医生不存在");
            }
        }
        LambdaQueryWrapper<Appointment> duplicateQw = new LambdaQueryWrapper<>();
        duplicateQw.eq(Appointment::getPatientId, existingAppointment.getPatientId())
                .eq(Appointment::getDoctorId, targetDoctorId)
                .eq(Appointment::getAppointmentDate, targetDate)
                .eq(Appointment::getStatus, 1)
                .ne(Appointment::getId, id);
        if (appointmentMapper.selectCount(duplicateQw) > 0) {
            throw new ServiceException("您已预约过该医生当天的门诊");
        }
        
        appointment.setId(id);
        appointment.setScheduleId(existingAppointment.getScheduleId() == null ? 0L : existingAppointment.getScheduleId());
        appointment.setAppointmentDate(targetDate);
        appointment.setTimeSlot(targetTimeSlot);
        appointment.setUpdateTime(LocalDateTime.now());
        
        if (appointmentMapper.updateById(appointment) <= 0) {
            throw new ServiceException("更新预约失败");
        }
    }
    
    /**
     * 生成预约编号
     */
    private String generateAppointmentNo() {
        // 生成格式：A + 年月日 + 6位随机数
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = String.format("%06d", (int)(Math.random() * 1000000));
        return "A" + dateStr + randomStr;
    }
    
    /**
     * 填充预约关联信息
     */
    private void fillAppointmentsInfo(List<Appointment> appointments) {
        if (appointments == null || appointments.isEmpty()) {
            return;
        }
        
        for (Appointment appointment : appointments) {
            // 查询患者信息
            Patient patient = patientMapper.selectById(appointment.getPatientId());
            appointment.setPatient(patient);
            
            // 查询医生信息
            Doctor doctor = doctorMapper.selectById(appointment.getDoctorId());
            if (doctor != null) {
                // 查询科室信息
                if (doctor.getDepartmentId() != null) {
                    Department department = departmentMapper.selectById(doctor.getDepartmentId());
                    doctor.setDepartment(department);
                }
                appointment.setDoctor(doctor);
            }
            
        }
    }

    private boolean isValidTimeSlot(String timeSlot) {
        return "上午".equals(timeSlot) || "下午".equals(timeSlot) || "晚上".equals(timeSlot);
    }
} 