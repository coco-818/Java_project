package com.example.dormmanagement.entity; // 确保这里的包名和你的匹配

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data // Lombok注解：自动生成getter/setter/toString
@TableName("user") // 映射数据库中的 user 表
public class User {

    @TableId(type = IdType.AUTO) // 声明主键自增
    private Long id;

    private String username;
    private String password;
    private String role; // ADMIN 或 USER

    private LocalDateTime createTime;
}