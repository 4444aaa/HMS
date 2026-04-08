package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springboot.entity.PurchaseSettlementOrder;

@Mapper
public interface PurchaseSettlementOrderMapper extends BaseMapper<PurchaseSettlementOrder> {
}
