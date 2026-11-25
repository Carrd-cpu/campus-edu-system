package com.scaffold.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scaffold.dto.NameCount;
import com.scaffold.entity.SysLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysLogMapper extends BaseMapper<SysLog> {

    @Select("""
            SELECT DATE_FORMAT(created_at, '%m-%d') AS name, COUNT(*) AS value
            FROM sys_log
            WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL #{days} - 1 DAY)
            GROUP BY DATE(created_at)
            ORDER BY DATE(created_at) ASC
            """)
    List<NameCount> countDailyCreated(@Param("days") int days);
}
