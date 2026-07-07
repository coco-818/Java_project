package com.example.dormmanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.dormmanagement.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper //
public interface UserMapper extends BaseMapper<User> {
    // 继承了 BaseMapper<User> 之后，自动拥有了对 user 表的增删改查功能
}