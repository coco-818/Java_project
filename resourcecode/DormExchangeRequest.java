package com.example.dormmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dorm_exchange_request") // 映射表名
public class DormExchangeRequest {

    @TableId(type = IdType.AUTO) // 主键自增
    private Long id;

    private String studentId;
    private String currentDormId;
    private String targetDormId;
    private String reason;
    private String status; // PENDING, APPROVED, REJECTED

    private LocalDateTime createTime;
}