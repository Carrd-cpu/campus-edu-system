package com.scaffold.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scaffold.entity.SysConfig;

import java.util.Map;

public interface ISysConfigService extends IService<SysConfig> {

    String KEY_SYSTEM_NAME = "system_name";
    String KEY_SYSTEM_LOGO = "system_logo";
    String KEY_LOGIN_COVER = "login_cover";
    String KEY_REGISTER_COVER = "register_cover";
    String KEY_ADMIN_LOGIN_COVER = "admin_login_cover";
    String DEFAULT_SYSTEM_NAME = "基础脚手架";
    String DEFAULT_LOGIN_COVER =
            "https://images.unsplash.com/photo-1522202176988-66273c2fd55f?auto=format&fit=crop&w=1400&q=80";
    String DEFAULT_REGISTER_COVER =
            "https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&w=1400&q=80";
    String DEFAULT_ADMIN_LOGIN_COVER =
            "https://images.unsplash.com/photo-1497366811353-6870744d04b2?auto=format&fit=crop&w=1400&q=80";

    Map<String, String> getPublicConfig();

    String getSystemName();

    String getSystemLogo();

    Map<String, String> updatePublicConfig(
            String systemName,
            String systemLogo,
            String loginCover,
            String registerCover,
            String adminLoginCover);
}
