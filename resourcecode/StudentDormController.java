package com.example.dormmanagement.controller;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dormmanagement.common.Result;
import com.example.dormmanagement.entity.StudentDorm;
import com.example.dormmanagement.mapper.StudentDormMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentDormController {

    private final StudentDormMapper studentDormMapper;

    /**
     * 1. 查询所有学生及宿舍信息
     */
    @GetMapping("/list")
    public Result<List<StudentDorm>> getAllStudents() {
        List<StudentDorm> list = studentDormMapper.selectList(null);
        return Result.success(list);
    }

    /**
     * 2. 根据学号查询单个学生
     */
    @GetMapping("/{studentId}")
    public Result<StudentDorm> getByStudentId(@PathVariable String studentId) {
        StudentDorm student = studentDormMapper.selectById(studentId);
        if (student == null) {
            return Result.error("未找到该学生信息！");
        }
        return Result.success(student);
    }

    /**
     * 3. 添加新入住学生
     */
    @PostMapping("/add")
    public Result<String> addStudentDorm(@RequestBody StudentDorm newStudent) {
        System.out.println("=== 收到新增请求 ===");
        System.out.println("宿舍号: " + newStudent.getDormId());
        System.out.println("床位号(bedNumber): " + newStudent.getBedNumber());
        //注意：如果这里打印出来床位号是 null，说明后端实体类属性名不是 bedNumber

        if (newStudent.getDormId() == null || newStudent.getDormId().trim().isEmpty()) {
            return Result.error("录入失败：宿舍号不能为空！");
        }

        //这里获取床位的方法，必须跟上面打印、以及实体类一致
        Object currentBedValue = newStudent.getBedNumber(); // 或者 getBedNo()
        if (currentBedValue == null || currentBedValue.toString().trim().isEmpty()) {
            return Result.error("录入失败：前端传输的床位号字段为空，请检查前后端字段对齐！");
        }

        QueryWrapper<StudentDorm> queryWrapper = new QueryWrapper<>();
        // 第一个参数必须是 MySQL 数据库里的真实列名
        queryWrapper.eq("dorm_id", newStudent.getDormId())
                .eq("bed_number", currentBedValue); //

        StudentDorm existingRecord = studentDormMapper.selectOne(queryWrapper);
        if (existingRecord != null) {
            return Result.error("【床位冲突警告】：" + newStudent.getDormId() + " 宿舍的 "
                    + currentBedValue + " 号床位已被学生 [" + existingRecord.getName() + "] 占用！");
        }

        int rows = studentDormMapper.insert(newStudent);
        return rows > 0 ? Result.success("学生住宿登记成功！") : Result.error("数据库写入失败！");
    }

    /**
     * 4. 修改学生宿舍信息
     */
    @PutMapping("/update")
    public Result<?> updateStudent(@RequestBody StudentDorm studentDorm) {
        studentDorm.setUpdateTime(LocalDateTime.now());
        studentDormMapper.updateById(studentDorm);
        return Result.success("信息修改成功");
    }

    /**
     * 5. 学生退宿（删除记录）
     */
    @DeleteMapping("/delete/{studentId}")
    public Result<?> deleteStudent(@PathVariable String studentId) {
        studentDormMapper.deleteById(studentId);
        return Result.success("退宿办理成功");
    }

    // 按照指定维度（deptName 或 className）进行升序排序
    @GetMapping("/list/sort")
    public Result<List<StudentDorm>> getStudentListSorted(@RequestParam String sortBy) {
        LambdaQueryWrapper<StudentDorm> wrapper = new LambdaQueryWrapper<>();
        if ("dept".equals(sortBy)) {
            wrapper.orderByAsc(StudentDorm::getDepartment);
        } else if ("class".equals(sortBy)) {
            wrapper.orderByAsc(StudentDorm::getClassName); // 按班级排序
        }
        List<StudentDorm> list = studentDormMapper.selectList(wrapper);
        return Result.success(list);
    }
}