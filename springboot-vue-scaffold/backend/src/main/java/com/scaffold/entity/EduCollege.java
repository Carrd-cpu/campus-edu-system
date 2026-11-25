package com.scaffold.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("edu_college")
public class EduCollege { // orm映射

    // 主键自增
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    private LocalDateTime createdAt;
}
