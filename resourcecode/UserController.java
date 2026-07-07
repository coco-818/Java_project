package com.example.dormmanagement.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dormmanagement.common.Result;
import com.example.dormmanagement.entity.User;
import com.example.dormmanagement.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    /**
     * 1. 登录接口
     */
    @PostMapping("/login")
    public Result<?> login(@RequestBody User user) {
        // 去数据库里查有没有这个用户名
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername());
        User loginUser = userMapper.selectOne(wrapper);

        // 判断用户是否存在以及密码是否正确
        if (loginUser == null || !loginUser.getPassword().equals(user.getPassword())) {
            return Result.error("用户名或密码错误！");
        }

        // 登录成功，把用户信息（包含角色）返回给前端
        return Result.success(loginUser);
    }

    /**
     * 2. 注册/添加用户接口
     */
    @PostMapping("/register")
    public Result<?> register(@RequestBody User user) {
        // 先检查用户名是否已经存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            return Result.error("用户名已存在！");
        }

        // 设置默认角色和创建时间
        if (user.getRole() == null) {
            user.setRole("USER"); // 默认是普通学生
        }
        user.setCreateTime(LocalDateTime.now());

        // 存入数据库
        userMapper.insert(user);
        return Result.success("注册成功");
    }
}