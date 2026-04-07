package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("purchase_plan")
@Schema(description = "采购计划主表")
public class PurchasePlan {
    @TableId(type = IdType.AUTO)
    @Schema(description = "采购计划ID")
    private Long id;

    @Schema(description = "计划编号")
    private String planNo;

    @Schema(description = "计划主题")
    private String title;

    @Schema(description = "创建人用户ID")
    private Long creatorUserId;

    @Schema(description = "状态(0:草稿,1:已提交,2:已完结)")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    @Schema(description = "计划明细")
    private List<PurchasePlanItem> items;
}

