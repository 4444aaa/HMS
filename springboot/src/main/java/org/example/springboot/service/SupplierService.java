package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.springboot.entity.Supplier;
import org.example.springboot.entity.SupplierMedicine;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.SupplierMedicineMapper;
import org.example.springboot.mapper.SupplierMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SupplierService {
    @Resource
    private SupplierMapper supplierMapper;
    @Resource
    private SupplierMedicineMapper supplierMedicineMapper;

    @Transactional
    public Supplier createSupplier(Supplier supplier) {
        if (supplier == null || StringUtils.isBlank(supplier.getName())) {
            throw new ServiceException("供应商名称不能为空");
        }
        if (StringUtils.isNotBlank(supplier.getSupplierCode())) {
            Supplier exist = supplierMapper.selectOne(
                    new LambdaQueryWrapper<Supplier>().eq(Supplier::getSupplierCode, supplier.getSupplierCode())
            );
            if (exist != null) {
                throw new ServiceException("供应商编码已存在");
            }
        }
        if (supplier.getStatus() == null) {
            supplier.setStatus(1);
        }
        LocalDateTime now = LocalDateTime.now();
        supplier.setCreateTime(now);
        supplier.setUpdateTime(now);
        if (supplierMapper.insert(supplier) <= 0) {
            throw new ServiceException("供应商创建失败");
        }
        return supplier;
    }

    @Transactional
    public void updateSupplier(Long id, Supplier supplier) {
        if (id == null) {
            throw new ServiceException("供应商ID不能为空");
        }
        Supplier db = supplierMapper.selectById(id);
        if (db == null) {
            throw new ServiceException("供应商不存在");
        }
        if (StringUtils.isNotBlank(supplier.getSupplierCode())) {
            Supplier exist = supplierMapper.selectOne(
                    new LambdaQueryWrapper<Supplier>().eq(Supplier::getSupplierCode, supplier.getSupplierCode())
            );
            if (exist != null && !exist.getId().equals(id)) {
                throw new ServiceException("供应商编码已被其他供应商使用");
            }
        }
        supplier.setId(id);
        supplier.setUpdateTime(LocalDateTime.now());
        if (supplierMapper.updateById(supplier) <= 0) {
            throw new ServiceException("供应商更新失败");
        }
    }

    public Supplier getSupplierById(Long id) {
        Supplier supplier = supplierMapper.selectById(id);
        if (supplier == null) {
            throw new ServiceException("供应商不存在");
        }
        return supplier;
    }

    @Transactional
    public void deleteSupplier(Long id) {
        if (supplierMapper.deleteById(id) <= 0) {
            throw new ServiceException("供应商删除失败");
        }
    }

    public Page<Supplier> getSuppliersByPage(String name, String supplierCode, Integer status,
                                             Integer currentPage, Integer size) {
        LambdaQueryWrapper<Supplier> qw = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(name)) {
            qw.like(Supplier::getName, name);
        }
        if (StringUtils.isNotBlank(supplierCode)) {
            qw.like(Supplier::getSupplierCode, supplierCode);
        }
        if (status != null) {
            qw.eq(Supplier::getStatus, status);
        }
        qw.orderByDesc(Supplier::getUpdateTime);
        return supplierMapper.selectPage(new Page<>(currentPage, size), qw);
    }

    public java.util.List<Long> getMedicineIdsBySupplierId(Long supplierId) {
        Supplier supplier = supplierMapper.selectById(supplierId);
        if (supplier == null) {
            throw new ServiceException("供应商不存在");
        }
        return supplierMedicineMapper.selectList(
                new LambdaQueryWrapper<SupplierMedicine>().eq(SupplierMedicine::getSupplierId, supplierId)
        ).stream().map(SupplierMedicine::getMedicineId).toList();
    }
}

