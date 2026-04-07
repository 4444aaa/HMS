package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("supplier_medicine")
public class SupplierMedicine {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long supplierId;
    private Long medicineId;
    private LocalDateTime createTime;
}

