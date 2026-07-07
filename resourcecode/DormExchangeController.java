package com.example.dormmanagement.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dormmanagement.common.Result;
import com.example.dormmanagement.entity.DormExchangeRequest;
import com.example.dormmanagement.entity.StudentDorm;
import com.example.dormmanagement.mapper.DormExchangeRequestMapper;
import com.example.dormmanagement.mapper.StudentDormMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/exchange")
@RequiredArgsConstructor
public class DormExchangeController {

    private final DormExchangeRequestMapper requestMapper;
    private final StudentDormMapper studentDormMapper;

    /**
     * 1. 学生提交调宿申请
     */
    @PostMapping("/apply")
    public Result<?> applyExchange(@RequestBody DormExchangeRequest request) {
        StudentDorm student = studentDormMapper.selectById(request.getStudentId());
        if (student == null) {
            return Result.error("学生学号不存在，无法申请！");
        }

        // 自动带出当前宿舍号
        request.setCurrentDormId(student.getDormId());
        request.setStatus("PENDING");
        request.setCreateTime(LocalDateTime.now());

        requestMapper.insert(request);
        return Result.success("申请提交成功，等待管理员审批");
    }

    /**
     * 2. 管理员查看所有申请列表
     */
    @GetMapping("/list")
    public Result<List<DormExchangeRequest>> getRequestList() {
        List<DormExchangeRequest> list = requestMapper.selectList(null);
        return Result.success(list);
    }

    /**
     * 3. 管理员审批申请
     */
    @PutMapping("/audit")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> auditRequest(@RequestBody AuditDTO dto) {
        DormExchangeRequest request = requestMapper.selectById(dto.getId());
        if (request == null) {
            return Result.error("申请记录不存在！");
        }
        request.setStatus(dto.getStatus());
        requestMapper.updateById(request);

        // 如果管理员点了同意(APPROVED)，自动去修改学生的宿舍号
        if ("APPROVED".equals(dto.getStatus())) {
            LambdaQueryWrapper<StudentDorm> studentWrapper = new LambdaQueryWrapper<>();
            studentWrapper.eq(StudentDorm::getStudentId, request.getStudentId());
            StudentDorm student = studentDormMapper.selectOne(studentWrapper);

            if (student != null) {
                student.setDormId(request.getTargetDormId());
                student.setUpdateTime(LocalDateTime.now());
                studentDormMapper.updateById(student);
            }
        }
        return Result.success("审批处理成功，学生宿舍已同步更新！");
    }
}

//添加 public 关键字，解决可见性域报错
@Data
class AuditDTO {
    private Long id;
    private String status;
}