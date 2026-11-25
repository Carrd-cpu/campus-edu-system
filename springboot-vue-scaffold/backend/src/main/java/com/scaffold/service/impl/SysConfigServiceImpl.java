package com.scaffold.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scaffold.common.BusinessException;
import com.scaffold.entity.SysConfig;
import com.scaffold.mapper.SysConfigMapper;
import com.scaffold.service.ISysConfigService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements ISysConfigService {

    @Override
    public Map<String, String> getPublicConfig() {
        Map<String, String> map = new HashMap<>();
        map.put("systemName", getSystemName());
        map.put("systemLogo", getSystemLogo());
        map.put("loginCover", firstNonBlank(getConfigValue(KEY_LOGIN_COVER), DEFAULT_LOGIN_COVER));
        map.put("registerCover", firstNonBlank(getConfigValue(KEY_REGISTER_COVER), DEFAULT_REGISTER_COVER));
        map.put("adminLoginCover", firstNonBlank(getConfigValue(KEY_ADMIN_LOGIN_COVER), DEFAULT_ADMIN_LOGIN_COVER));
        return map;
    }

    private String firstNonBlank(String value, String fallback) {
        return StrUtil.isBlank(value) ? fallback : value;
    }

    @Override
    public String getSystemName() {
        SysConfig config = getById(KEY_SYSTEM_NAME);
        if (config == null || StrUtil.isBlank(config.getConfigValue())) {
            return DEFAULT_SYSTEM_NAME;
        }
        return config.getConfigValue().trim();
    }

    @Override
    public String getSystemLogo() {
        return getConfigValue(KEY_SYSTEM_LOGO);
    }

    private String getConfigValue(String key) {
        SysConfig config = getById(key);
        if (config == null || StrUtil.isBlank(config.getConfigValue())) {
            return "";
        }
        return config.getConfigValue().trim();
    }

    @Override
    public Map<String, String> updatePublicConfig(
            String systemName,
            String systemLogo,
            String loginCover,
            String registerCover,
            String adminLoginCover) {
        if (StrUtil.isBlank(systemName)) {
            throw new BusinessException("系统名称不能为空");
        }
        String name = systemName.trim();
        if (name.length() > 40) {
            throw new BusinessException("系统名称不能超过 40 个字符");
        }
        upsert(KEY_SYSTEM_NAME, name, "系统名称");
        upsertMedia(KEY_SYSTEM_LOGO, systemLogo, "系统 Logo");
        upsertMedia(KEY_LOGIN_COVER, loginCover, "登录封面");
        upsertMedia(KEY_REGISTER_COVER, registerCover, "注册封面");
        upsertMedia(KEY_ADMIN_LOGIN_COVER, adminLoginCover, "后台登录封面");
        return getPublicConfig();
    }

    private void upsertMedia(String key, String value, String remark) {
        String media = value == null ? "" : value.trim();
        if (media.length() > 1000) {
            throw new BusinessException(remark + "地址过长");
        }
        upsert(key, media, remark);
    }

    private void upsert(String key, String value, String remark) {
        SysConfig existing = getById(key);
        if (existing == null) {
            SysConfig config = new SysConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setRemark(remark);
            save(config);
        } else {
            existing.setConfigValue(value);
            existing.setRemark(remark);
            updateById(existing);
        }
    }
}
