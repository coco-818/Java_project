package com.example.dormmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("student_dorm") // 映射 student_dorm 表
public class StudentDorm {

    @TableId // 学号是主键（注意：不是自增，是手工录入的字符串）
    private String studentId;

    private String name;
    private String department; // 所在系
    private String className;  // 班级
    private String dormId;     // 宿舍号
    private String phone;
    private Integer bedNumber; // 床位号

    private LocalDateTime updateTime;
}