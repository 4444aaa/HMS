package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("medical_record_detail")
@Schema(description = "就诊记录病症明细实体类")
public class MedicalRecordDetail {
    @TableId(type = IdType.AUTO)
    @Schema(description = "就诊明细ID")
    private Long id;

    @Schema(description = "就诊记录ID")
    private Long recordId;

    @Schema(description = "病症名称")
    private String symptomName;

    @Schema(description = "治疗方案")
    private String treatmentPlan;

    @Schema(description = "排序号")
    private Integer sortNo;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    @Schema(description = "关联就诊记录")
    private MedicalRecord medicalRecord;
}
