package com.scaffold.controller.user;

import com.scaffold.common.Result;
import com.scaffold.service.ISysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/user/system-config")
public class UserSystemConfigController {

    @Autowired
    private ISysConfigService sysConfigService;

    @GetMapping
    public Result<Map<String, String>> get() {
        return Result.ok(sysConfigService.getPublicConfig());
    }
}
