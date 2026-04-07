package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springboot.entity.MedicalRecordDetail;

@Mapper
public interface MedicalRecordDetailMapper extends BaseMapper<MedicalRecordDetail> {
}
