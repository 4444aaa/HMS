package org.example.springboot.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.example.springboot.entity.Patient;
import org.example.springboot.entity.Supplier;
import org.example.springboot.entity.User;
import org.example.springboot.mapper.PatientMapper;
import org.example.springboot.mapper.SupplierMapper;
import org.example.springboot.mapper.UserMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 列表组合查询：按姓名/关键词解析为用户、供应商、患者 ID 集合。
 */
@Component
public class ListQuerySupport {
    @Resource
    private UserMapper userMapper;
    @Resource
    private SupplierMapper supplierMapper;
    @Resource
    private PatientMapper patientMapper;

    /**
     * @param keyword 姓名或用户名模糊关键字
     * @return null 表示不筛选；空列表表示无匹配用户（列表应直接空页）
     */
    public List<Long> resolveUserIdsByKeyword(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return null;
        }
        String q = keyword.trim();
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .and(w -> w.like(User::getName, q).or().like(User::getUsername, q)));
        return users.stream().map(User::getId).distinct().toList();
    }

    /**
     * @return null 不筛选；空列表无匹配
     */
    public List<Long> resolveSupplierIdsByName(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        String q = name.trim();
        List<Supplier> list = supplierMapper.selectList(
                new LambdaQueryWrapper<Supplier>().like(Supplier::getName, q));
        return list.stream().map(Supplier::getId).distinct().toList();
    }

    /**
     * @return null 不筛选；空列表无匹配
     */
    public List<Long> resolvePatientIdsByKeyword(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return null;
        }
        String q = keyword.trim();
        List<Patient> list = patientMapper.selectList(new LambdaQueryWrapper<Patient>()
                .and(w -> w.like(Patient::getName, q).or().like(Patient::getPatientNo, q)));
        return list.stream().map(Patient::getId).distinct().toList();
    }

    public static <T> void applyCreateTimeDateRange(LambdaQueryWrapper<T> qw,
                                                    SFunction<T, LocalDateTime> createTimeColumn,
                                                    LocalDate start,
                                                    LocalDate end) {
        if (start != null) {
            qw.ge(createTimeColumn, start.atStartOfDay());
        }
        if (end != null) {
            qw.le(createTimeColumn, LocalDateTime.of(end, LocalTime.MAX));
        }
    }
}
