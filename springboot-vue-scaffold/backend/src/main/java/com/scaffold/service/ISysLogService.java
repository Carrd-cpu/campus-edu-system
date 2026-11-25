package com.scaffold.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scaffold.common.PageResult;
import com.scaffold.entity.SysLog;
import jakarta.servlet.http.HttpServletRequest;

public interface ISysLogService extends IService<SysLog> {

    PageResult<SysLog> page(String keyword, String type, int pageNum, int pageSize);

    void log(Long userId, String username, String type, String module, String action, String detail);

    void logOperation(HttpServletRequest request, String module, String action, String detail);

    void delete(Long id);

    void ensureDemoLogs();
}
