package com.example.dormmanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dormmanagement.entity.StudentDorm;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudentDormMapper extends BaseMapper<StudentDorm> {
}