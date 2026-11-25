package com.scaffold.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动时创建基础表，并写入默认管理员、演示用户、菜单和系统名称。
 */
@Component
@Order(1)
public class SchemaRunner implements ApplicationRunner {

    @Autowired
    private JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        createTables();
        seedAdmin();
        seedUser();
        seedMenus();
        seedConfig();
        seedBanners();
    }

    private void createTables() {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS `admin` (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  username VARCHAR(64) NOT NULL,
                  password VARCHAR(128) NOT NULL,
                  nickname VARCHAR(64) DEFAULT NULL,
                  avatar VARCHAR(255) DEFAULT NULL,
                  phone VARCHAR(32) DEFAULT NULL,
                  status INT DEFAULT 1,
                  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_admin_username (username)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS `user` (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  username VARCHAR(64) NOT NULL,
                  password VARCHAR(128) NOT NULL,
                  nickname VARCHAR(64) DEFAULT NULL,
                  avatar VARCHAR(255) DEFAULT NULL,
                  phone VARCHAR(32) DEFAULT NULL,
                  role VARCHAR(32) DEFAULT 'USER',
                  status INT DEFAULT 1,
                  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (id),
                  UNIQUE KEY uk_user_username (username)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS sys_menu (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  parent_id BIGINT DEFAULT 0,
                  title VARCHAR(64) NOT NULL,
                  path VARCHAR(128) DEFAULT NULL,
                  icon VARCHAR(64) DEFAULT NULL,
                  sort_order INT DEFAULT 0,
                  status INT DEFAULT 1,
                  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS sys_log (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  user_id BIGINT DEFAULT NULL,
                  username VARCHAR(64) DEFAULT NULL,
                  type VARCHAR(32) DEFAULT NULL,
                  module VARCHAR(64) DEFAULT NULL,
                  action VARCHAR(64) DEFAULT NULL,
                  ip VARCHAR(64) DEFAULT NULL,
                  detail VARCHAR(500) DEFAULT NULL,
                  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS sys_config (
                  config_key VARCHAR(64) NOT NULL,
                  config_value VARCHAR(1000) DEFAULT NULL,
                  remark VARCHAR(128) DEFAULT NULL,
                  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (config_key)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        try {
            jdbc.execute("ALTER TABLE sys_config MODIFY COLUMN config_value VARCHAR(1000) DEFAULT NULL");
        } catch (Exception ignored) {
            // 已是目标长度时忽略
        }
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS home_banner (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  title VARCHAR(128) DEFAULT NULL,
                  subtitle VARCHAR(255) DEFAULT NULL,
                  cover VARCHAR(255) DEFAULT NULL,
                  link_url VARCHAR(255) DEFAULT NULL,
                  cta_text VARCHAR(64) DEFAULT NULL,
                  sort_order INT DEFAULT 0,
                  status INT DEFAULT 1,
                  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void seedAdmin() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM admin WHERE username = ?", Integer.class, "admin");
        if (count != null && count > 0) {
            return;
        }
        jdbc.update("""
                INSERT INTO admin(username, password, nickname, status, created_at, updated_at)
                VALUES ('admin', '123456', '系统管理员', 1, NOW(), NOW())
                """);
    }

    private void seedUser() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM `user` WHERE username = ?", Integer.class, "user");
        if (count != null && count > 0) {
            return;
        }
        jdbc.update("""
                INSERT INTO `user`(username, password, nickname, role, status, created_at, updated_at)
                VALUES ('user', '123456', '演示用户', 'USER', 1, NOW(), NOW())
                """);
    }

    private void seedMenus() {
        ensureMenu(0L, "首页", "/admin/home", "HomeFilled", 1);
        long systemId = ensureMenu(0L, "系统管理", "/admin/system", "Setting", 2);
        ensureMenu(systemId, "管理员管理", "/admin/admins", "Avatar", 1);
        ensureMenu(systemId, "用户管理", "/admin/users", "User", 2);
        ensureMenu(systemId, "菜单管理", "/admin/menus", "Grid", 3);
        ensureMenu(systemId, "轮播图管理", "/admin/banners", "Picture", 4);
        ensureMenu(systemId, "系统日志", "/admin/logs", "Tickets", 5);
        ensureMenu(systemId, "系统设置", "/admin/settings", "Tools", 6);
    }

    private long ensureMenu(long parentId, String title, String path, String icon, int sortOrder) {
        Long id = jdbc.query("""
                SELECT id FROM sys_menu WHERE path = ? LIMIT 1
                """, rs -> rs.next() ? rs.getLong(1) : null, path);
        if (id != null) {
            return id;
        }
        jdbc.update("""
                INSERT INTO sys_menu(parent_id, title, path, icon, sort_order, status, created_at)
                VALUES (?, ?, ?, ?, ?, 1, NOW())
                """, parentId, title, path, icon, sortOrder);
        Long created = jdbc.queryForObject("SELECT id FROM sys_menu WHERE path = ? LIMIT 1", Long.class, path);
        return created == null ? 0L : created;
    }

    private void seedConfig() {
        ensureConfig("system_name", "基础脚手架", "系统名称");
        ensureConfig(
                "login_cover",
                "https://images.unsplash.com/photo-1522202176988-66273c2fd55f?auto=format&fit=crop&w=1400&q=80",
                "登录封面");
        ensureConfig(
                "register_cover",
                "https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&w=1400&q=80",
                "注册封面");
        ensureConfig(
                "admin_login_cover",
                "https://images.unsplash.com/photo-1497366811353-6870744d04b2?auto=format&fit=crop&w=1400&q=80",
                "后台登录封面");
    }

    private void ensureConfig(String key, String value, String remark) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_config WHERE config_key = ?", Integer.class, key);
        if (count != null && count > 0) {
            return;
        }
        jdbc.update("""
                INSERT INTO sys_config(config_key, config_value, remark, updated_at)
                VALUES (?, ?, ?, NOW())
                """, key, value, remark);
    }

    private void seedBanners() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM home_banner", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        jdbc.update("""
                INSERT INTO home_banner(title, subtitle, cover, link_url, cta_text, sort_order, status, created_at, updated_at)
                VALUES
                (?, ?, ?, ?, ?, 1, 1, NOW(), NOW()),
                (?, ?, ?, ?, ?, 2, 1, NOW(), NOW()),
                (?, ?, ?, ?, ?, 3, 1, NOW(), NOW())
                """,
                "欢迎使用基础脚手架",
                "登录注册、个人中心、后台管理，一套通用底座。",
                "https://images.unsplash.com/photo-1522202176988-66273c2fd55f?auto=format&fit=crop&w=1600&q=80",
                "/login",
                "立即登录",
                "完善你的个人资料",
                "头像、昵称和密码都可以在个人中心随时修改。",
                "https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&w=1600&q=80",
                "/user/profile",
                "进入个人中心",
                "后台统一配置",
                "菜单、轮播图和系统设置，都在管理端集中管理。",
                "https://images.unsplash.com/photo-1497366811353-6870744d04b2?auto=format&fit=crop&w=1600&q=80",
                "/admin/login",
                "管理员入口");
    }
}
