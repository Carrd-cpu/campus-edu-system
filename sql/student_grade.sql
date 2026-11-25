/*
 Navicat Premium Data Transfer

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 50737
 Source Host           : localhost:3306
 Source Schema         : student_grade

 Target Server Type    : MySQL
 Target Server Version : 50737
 File Encoding         : 65001

 Date: 13/10/2026 16:40:54
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for admin
-- ----------------------------
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `avatar` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of admin
-- ----------------------------
INSERT INTO `admin` VALUES (1, 'admin', 'admin123', '超级管理员', '/avatars/admin.jpg', '', 1, '2026-07-24 15:39:49', '2026-10-13 10:25:07');
INSERT INTO `admin` VALUES (2, 'eduadmin', '123456', '教务管理员', '/avatars/eduadmin.jpg', NULL, 1, '2026-10-08 13:29:38', '2026-10-13 10:25:07');

-- ----------------------------
-- Table structure for home_banner
-- ----------------------------
DROP TABLE IF EXISTS `home_banner`;
CREATE TABLE `home_banner`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `subtitle` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `cover` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `link_url` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `cta_text` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '了解更多',
  `sort_order` int(11) NULL DEFAULT 0,
  `status` tinyint(4) NOT NULL DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of home_banner
-- ----------------------------
INSERT INTO `home_banner` VALUES (1, 'Welcome to Nexora', 'Spring Boot 3 + Vue 3 management starter', 'https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=1600&q=80', 'https://example.com', '了解更多', 1, 1, '2026-07-24 15:39:49', '2026-07-24 15:56:28');
INSERT INTO `home_banner` VALUES (2, '开箱即用', '用户、菜单、日志、设置一应俱全', 'https://images.unsplash.com/photo-1461749280684-dccba630e2f6?auto=format&fit=crop&w=1600&q=80', 'https://example.com/docs', '查看文档', 2, 1, '2026-07-24 15:39:49', '2026-07-24 15:39:49');

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
  `config_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `config_value` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`config_key`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_config
-- ----------------------------
INSERT INTO `sys_config` VALUES ('admin_login_cover', '', '后台登录封面', '2026-10-12 15:51:20');
INSERT INTO `sys_config` VALUES ('login_cover', '', '登录封面', '2026-10-12 15:51:20');
INSERT INTO `sys_config` VALUES ('register_cover', '', '注册封面', '2026-10-12 15:51:20');
INSERT INTO `sys_config` VALUES ('system_logo', '', '系统 Logo', '2026-07-24 16:33:42');
INSERT INTO `sys_config` VALUES ('system_name', '校园教务系统', '系统名称', '2026-07-24 16:33:42');

-- ----------------------------
-- Table structure for sys_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_log`;
CREATE TABLE `sys_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NULL DEFAULT NULL,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `module` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `action` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 177 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_log
-- ----------------------------
INSERT INTO `sys_log` VALUES (10, 1, 'user', 'LOGIN', '用户认证', '用户登录', '127.0.0.1', '登录成功', '2026-07-24 15:45:22');
INSERT INTO `sys_log` VALUES (11, 1, 'user', 'LOGIN', '用户认证', '用户登录', '127.0.0.1', '登录成功', '2026-07-24 15:48:51');
INSERT INTO `sys_log` VALUES (12, 1, 'user', 'LOGIN', '用户认证', '用户登录', '127.0.0.1', '登录成功', '2026-07-24 15:52:08');
INSERT INTO `sys_log` VALUES (13, 1, 'admin', 'LOGIN', '后台认证', '管理员登录', '127.0.0.1', '登录成功', '2026-07-24 15:57:04');
INSERT INTO `sys_log` VALUES (15, 3, 'tom', 'REGISTER', '用户认证', '用户注册', '127.0.0.1', '注册成功', '2026-07-24 16:05:35');
INSERT INTO `sys_log` VALUES (16, 1, 'admin', 'LOGIN', '后台认证', '管理员登录', '127.0.0.1', '登录成功', '2026-07-24 16:05:57');
INSERT INTO `sys_log` VALUES (17, 1, 'admin', 'LOGIN', '后台认证', '管理员登录', '127.0.0.1', '登录成功', '2026-07-24 16:15:30');
INSERT INTO `sys_log` VALUES (18, 1, 'admin', 'LOGIN', '后台认证', '管理员登录', '127.0.0.1', '登录成功', '2026-07-24 16:17:45');

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `parent_id` bigint(20) NULL DEFAULT 0,
  `title` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `path` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `icon` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `sort_order` int(11) NULL DEFAULT 0,
  `status` tinyint(4) NOT NULL DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 22 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (1, 0, '首页', '/admin/home', 'HomeFilled', 1, 1, '2026-07-24 15:39:49');
INSERT INTO `sys_menu` VALUES (2, 8, '管理员管理', '/admin/admins', 'Avatar', 1, 1, '2026-07-24 15:39:49');
INSERT INTO `sys_menu` VALUES (3, 8, '用户管理', '/admin/users', 'User', 2, 1, '2026-07-24 15:39:49');
INSERT INTO `sys_menu` VALUES (4, 8, '菜单管理', '/admin/menus', 'Grid', 3, 1, '2026-07-24 15:39:49');
INSERT INTO `sys_menu` VALUES (6, 8, '系统日志', '/admin/logs', 'Tickets', 5, 1, '2026-07-24 15:39:49');
INSERT INTO `sys_menu` VALUES (7, 8, '系统设置', '/admin/settings', 'Tools', 6, 1, '2026-07-24 15:39:49');
INSERT INTO `sys_menu` VALUES (8, 0, '系统管理', '/admin/system', 'Setting', 2, 1, '2026-07-24 16:33:52');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `avatar` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'USER',
  `status` tinyint(4) NOT NULL DEFAULT 1,
  `openid` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `balance` decimal(10, 2) NOT NULL DEFAULT 0.00,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `real_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `student_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `clazz_id` bigint(20) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username`) USING BTREE,
  UNIQUE INDEX `openid`(`openid`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, 'user', 'user123', '演示用户', '/avatars/user.jpg', NULL, 'USER', 1, NULL, 0.00, '2026-07-24 15:39:49', '2026-10-13 10:59:24', '普通用户', NULL, NULL);
INSERT INTO `user` VALUES (3, 'tom', '123456', 'tom', '/avatars/tom.jpg', '15098765321', 'USER', 1, NULL, 0.00, '2026-07-24 16:05:35', '2026-10-13 10:59:24', '汤姆', NULL, NULL);
INSERT INTO `user` VALUES (4, 'linxiao', '123456', '林晓', '/avatars/linxiao.jpg', NULL, 'USER', 1, NULL, 0.00, '2026-10-08 13:25:08', '2026-10-13 10:25:07', '林晓', '2022010101', 1);

SET FOREIGN_KEY_CHECKS = 1;
