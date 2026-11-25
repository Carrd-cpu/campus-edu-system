package com.scaffold.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scaffold.common.PageResult;
import com.scaffold.entity.SysLog;
import com.scaffold.mapper.SysLogMapper;
import com.scaffold.service.ISysLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class SysLogServiceImpl extends ServiceImpl<SysLogMapper, SysLog> implements ISysLogService {

    /**
     * 分页查询
     */
    @Override
    public PageResult<SysLog> page(String keyword, String type, int pageNum, int pageSize) {
        LambdaQueryWrapper<SysLog> queryWrapper = new LambdaQueryWrapper<SysLog>().orderByDesc(SysLog::getId);
        queryWrapper.eq(StrUtil.isNotBlank(type), SysLog::getType, type);
        queryWrapper.and(StrUtil.isNotBlank(keyword), w -> w.like(SysLog::getUsername, keyword)
                .or().like(SysLog::getModule, keyword)
                .or().like(SysLog::getAction, keyword)
                .or().like(SysLog::getDetail, keyword));
        return PageResult.of(page(new Page<>(pageNum, pageSize), queryWrapper));
    }

    @Override
    public void log(Long userId, String username, String type, String module, String action, String detail) {
        SysLog row = new SysLog();
        row.setUserId(userId);
        row.setUsername(username);
        row.setType(type);
        row.setModule(module);
        row.setAction(action);
        row.setDetail(detail);
        row.setIp(resolveClientIp());
        save(row);
    }

    @Override
    public void logOperation(HttpServletRequest request, String module, String action, String detail) {
        Long userId = null;
        String username = null;
        if (request != null) {
            Object id = request.getAttribute("userId");
            if (id instanceof Long longId) {
                userId = longId;
            } else if (id instanceof Integer intId) {
                userId = intId.longValue();
            }
            Object name = request.getAttribute("username");
            if (name instanceof String text) {
                username = text;
            }
        }
        log(userId, username, "OPERATION", module, action, detail);
    }

    /**
     * 删除
     */
    @Override
    public void delete(Long id) {
        removeById(id);
    }

    @Override
    public void ensureDemoLogs() {
        if (count() > 0) {
            return;
        }
        insertDemo("admin", "LOGIN", "认证中心", "管理员登录", "演示：后台账号登录成功");
        insertDemo("admin", "OPERATION", "用户管理", "新增用户", "演示：创建用户账号");
        insertDemo("admin", "OPERATION", "菜单管理", "修改菜单", "演示：调整侧栏菜单");
        insertDemo("user", "LOGIN", "用户认证", "用户登录", "演示：前台用户登录成功");
        insertDemo("admin", "OPERATION", "系统设置", "更新配置", "演示：调整站点基础配置");
    }

    private void insertDemo(String username, String type, String module, String action, String detail) {
        SysLog row = new SysLog();
        row.setUsername(username);
        row.setType(type);
        row.setModule(module);
        row.setAction(action);
        row.setDetail(detail);
        row.setIp("127.0.0.1");
        save(row);
    }

    private String resolveClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isBlank()) {
                    ip = request.getRemoteAddr();
                } else {
                    int comma = ip.indexOf(',');
                    if (comma > 0) {
                        ip = ip.substring(0, comma).trim();
                    }
                }
                return ip;
            }
        } catch (Exception ignored) {
            // 取不到请求时记 unknown
        }
        return "unknown";
    }
}
