package com.scaffold.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.dto.DashboardStats;
import com.scaffold.dto.NameCount;
import com.scaffold.entity.Admin;
import com.scaffold.entity.SysLog;
import com.scaffold.entity.User;
import com.scaffold.mapper.AdminMapper;
import com.scaffold.mapper.SysLogMapper;
import com.scaffold.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private static final int TREND_DAYS = 7;
    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("MM-dd");

    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private SysLogMapper sysLogMapper;

    public DashboardStats overview() {
        DashboardStats stats = new DashboardStats();
        stats.setAdminCount(adminMapper.selectCount(new LambdaQueryWrapper<Admin>()));
        stats.setUserCount(userMapper.selectCount(new LambdaQueryWrapper<User>()));
        stats.setLogCount(sysLogMapper.selectCount(new LambdaQueryWrapper<SysLog>()));
        stats.setUserTrend(fillDailyTrend(userMapper.countDailyCreated(TREND_DAYS)));
        stats.setLogTrend(fillDailyTrend(sysLogMapper.countDailyCreated(TREND_DAYS)));
        return stats;
    }

    private List<DashboardStats.TrendPoint> fillDailyTrend(List<NameCount> raw) {
        Map<String, Long> map = new HashMap<>();
        if (raw != null) {
            for (NameCount item : raw) {
                if (item.getName() != null) {
                    map.put(item.getName(), item.getValue());
                }
            }
        }
        List<DashboardStats.TrendPoint> points = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = TREND_DAYS - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            String key = day.format(LABEL);
            points.add(new DashboardStats.TrendPoint(key, map.getOrDefault(key, 0L)));
        }
        return points;
    }
}
