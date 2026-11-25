package com.scaffold.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.entity.SysMenu;
import com.scaffold.mapper.SysMenuMapper;
import com.scaffold.service.ISysLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SysLogSeedRunner implements ApplicationRunner {

    @Autowired
    private ISysLogService sysLogService;
    @Autowired
    private SysMenuMapper sysMenuMapper;

    @Override
    public void run(ApplicationArguments args) {
        sysLogService.ensureDemoLogs();
        ensureLogsMenu();
    }

    private void ensureLogsMenu() {
        Long exists = sysMenuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getPath, "/admin/logs"));
        if (exists != null && exists > 0) {
            return;
        }
        SysMenu parent = sysMenuMapper.selectOne(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getPath, "/admin/system")
                .last("LIMIT 1"));
        SysMenu menu = new SysMenu();
        menu.setParentId(parent != null ? parent.getId() : 0L);
        menu.setTitle("系统日志");
        menu.setPath("/admin/logs");
        menu.setIcon("Tickets");
        menu.setSortOrder(5);
        menu.setStatus(1);
        sysMenuMapper.insert(menu);
    }
}
