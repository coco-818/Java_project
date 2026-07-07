package com.example.dormmanagement.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dormmanagement.common.Result;
import com.example.dormmanagement.entity.StudentDorm;
import com.example.dormmanagement.mapper.StudentDormMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final StudentDormMapper studentDormMapper;

    @GetMapping("/ai")
    public Result<String> getAiReport(@RequestParam(required = false) String dormId) {
        // 1. 从数据库中查询学生住宿数据
        LambdaQueryWrapper<StudentDorm> wrapper = new LambdaQueryWrapper<>();
        // 如果前端传了具体的宿舍号，就只统计该宿舍；没传就统计全校/全栋楼
        if (dormId != null && !dormId.trim().isEmpty()) {
            wrapper.eq(StudentDorm::getDormId, dormId);
        }
        List<StudentDorm> studentList = studentDormMapper.selectList(wrapper);

        // 2. 内存计算/统计各项指标
        int totalStudents = studentList.size();

        // 统计各院系的人数分布
        Map<String, Long> deptDistribution = studentList.stream()
                .filter(s -> s.getDepartment() != null)
                .collect(Collectors.groupingBy(StudentDorm::getDepartment, Collectors.counting()));

        // 统计涉及到的宿舍总数
        long totalDormsCount = studentList.stream()
                .map(StudentDorm::getDormId)
                .distinct()
                .count();

        // 3. 构建发送给大模型的精细 Prompt
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个高校宿舍管理专家。请根据以下真实的宿舍统计数据，自动生成一段200字左右的“宿舍运营评估与建议”，要求语气专业、一针见血，包含现状分析和未来规划建议。\n\n");

        if (dormId != null && !dormId.trim().isEmpty()) {
            prompt.append("【统计范围】：指定宿舍号为 ").append(dormId).append(" 的数据\n");
        } else {
            prompt.append("【统计范围】：全校全栋所有宿舍总体数据\n");
        }
        prompt.append("【当前入住总人数】：").append(totalStudents).append(" 人\n");
        prompt.append("【涉及宿舍总间数】：").append(totalDormsCount).append(" 间\n");
        prompt.append("【各院系入住人数分布】：").append(deptDistribution.toString()).append("\n");
        prompt.append("【假设背景】：假设每间宿舍标准床位为4人。请根据以上数据推算满员率、床位紧张程度，并给出优化宿舍资源分配的建议。");

        // 4. 发送 HTTP 请求调用大模型接口
        try {
            RestTemplate restTemplate = new RestTemplate();
            String apiUrl = "https://api.deepseek.com/v1/chat/completions"; //
            String apiKey = "Bearer sk-9acaf81acb254ae89412b299c255c83b";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", apiKey);

            // 组装标准的 Chat 传参格式
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-v4-flash"); //

            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt.toString());

            requestBody.put("messages", new Object[]{message});
            requestBody.put("temperature", 0.7);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);

            // 解析大模型返回的 JSON，提取 text 文本
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map body = response.getBody();
                List choices = (List) body.get("choices");
                Map firstChoice = (Map) choices.get(0);
                Map resMessage = (Map) firstChoice.get("message");
                String aiResult = (String) resMessage.get("content");

                return Result.success(aiResult);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("AI 引擎分析时发生通讯故障，请检查后端 API 密钥或网络。");
        }

        return Result.error("AI 引擎未能生成有效报告。");
    }
}