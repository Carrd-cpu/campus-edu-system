package com.scaffold.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_config")
public class SysConfig {

    @TableId(value = "config_key", type = IdType.INPUT)
    private String configKey;
    private String configValue;
    private String remark;
    private LocalDateTime updatedAt;
}
