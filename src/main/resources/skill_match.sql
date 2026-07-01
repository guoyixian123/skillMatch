/*
 Navicat Premium Dump SQL

 Source Server         : mysqlControl
 Source Server Type    : MySQL
 Source Server Version : 80100 (8.1.0)
 Source Host           : localhost:3306
 Source Schema         : skill_match

 Target Server Type    : MySQL
 Target Server Version : 80100 (8.1.0)
 File Encoding         : 65001

 Date: 01/07/2026 18:26:09
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for admin_user
-- ----------------------------
DROP TABLE IF EXISTS `admin_user`;
CREATE TABLE `admin_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` varchar(64) NOT NULL COMMENT '登录账号',
  `name` varchar(32) NOT NULL,
  `password` varchar(64) NOT NULL COMMENT 'MD5',
  `role` varchar(20) DEFAULT 'ADMIN' COMMENT 'ROOT|ADMIN',
  `parent_id` bigint DEFAULT NULL,
  `status` tinyint(1) DEFAULT '1',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of admin_user
-- ----------------------------
INSERT INTO `admin_user` (`user_id`, `name`, `password`, `role`, `parent_id`, `status`, `created_at`, `updated_at`) VALUES ('admin', '超级管理员', 'e10adc3949ba59abbe56e057f20f883e', 'ROOT', NULL, 1, NOW(), NOW());

-- ----------------------------
-- Table structure for chat_message
-- ----------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
  `id` varchar(64) NOT NULL,
  `from_user_id` varchar(64) NOT NULL,
  `to_user_id` varchar(64) NOT NULL,
  `content` varchar(2000) NOT NULL,
  `is_read` tinyint(1) DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_from_to` (`from_user_id`,`to_user_id`),
  KEY `idx_to_read` (`to_user_id`,`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for contact_request
-- ----------------------------
DROP TABLE IF EXISTS `contact_request`;
CREATE TABLE `contact_request` (
  `id` varchar(255) NOT NULL,
  `to_user_id` varchar(255) NOT NULL,
  `from_user_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `reason` varchar(255) DEFAULT NULL COMMENT '请求理由',
  `status` tinyint DEFAULT '1' COMMENT '待定 / 已接受 / 已拒绝',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`,`to_user_id`,`from_user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for friend
-- ----------------------------
DROP TABLE IF EXISTS `friend`;
CREATE TABLE `friend` (
  `id` varchar(64) NOT NULL,
  `user_id` varchar(64) NOT NULL,
  `friend_id` varchar(64) NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_friend` (`user_id`,`friend_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for hobby_tag
-- ----------------------------
DROP TABLE IF EXISTS `hobby_tag`;
CREATE TABLE `hobby_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL COMMENT '爱好名称',
  `icon` varchar(20) DEFAULT '✨' COMMENT 'emoji图标',
  `category` varchar(50) NOT NULL COMMENT '分类',
  `sort_order` int DEFAULT '0' COMMENT '排序权重',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='爱好标签字典表';

-- ----------------------------
-- Table structure for like_info
-- ----------------------------
DROP TABLE IF EXISTS `like_info`;
CREATE TABLE `like_info` (
  `id` varchar(255) NOT NULL,
  `user_id` varchar(255) NOT NULL,
  `biz_id` varchar(255) NOT NULL,
  `type` int DEFAULT NULL COMMENT '个人主页1/社区2',
  PRIMARY KEY (`id`,`user_id`,`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for notification
-- ----------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `receiver_id` varchar(64) NOT NULL COMMENT '接收者 userId',
  `actor_id` varchar(64) NOT NULL COMMENT '触发者 userId',
  `type` tinyint NOT NULL COMMENT '1=主页被赞 2=帖子被赞',
  `biz_id` varchar(64) DEFAULT NULL COMMENT '关联实体ID',
  `is_read` tinyint(1) DEFAULT '0' COMMENT '0=未读 1=已读',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_receiver_read` (`receiver_id`,`is_read`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=53890 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for post
-- ----------------------------
DROP TABLE IF EXISTS `post`;
CREATE TABLE `post` (
  `id` varchar(255) NOT NULL,
  `author_id` varchar(255) NOT NULL,
  `title` varchar(128) NOT NULL,
  `body` text,
  `like_count` int DEFAULT '0',
  `comment_count` int DEFAULT '0',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`,`author_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for post_comment
-- ----------------------------
DROP TABLE IF EXISTS `post_comment`;
CREATE TABLE `post_comment` (
  `id` varchar(255) NOT NULL,
  `post_id` varchar(36) NOT NULL,
  `user_id` varchar(36) NOT NULL,
  `body` varchar(255) NOT NULL,
  `created_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`,`post_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for post_tag
-- ----------------------------
DROP TABLE IF EXISTS `post_tag`;
CREATE TABLE `post_tag` (
  `id` varchar(255) NOT NULL,
  `post_id` varchar(255) NOT NULL,
  `tag_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`,`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for skill_tag
-- ----------------------------
DROP TABLE IF EXISTS `skill_tag`;
CREATE TABLE `skill_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL COMMENT '技能名称',
  `category` varchar(50) NOT NULL COMMENT '分类',
  `sort_order` int DEFAULT '0' COMMENT '排序权重',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=56 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='技能标签字典表';

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `user_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '账号id',
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户姓名',
  `password` varchar(128) NOT NULL COMMENT '账号密码',
  `contact_info` varchar(60) DEFAULT NULL COMMENT '联系方式,如微信号,qq号',
  `head_url` varchar(255) DEFAULT NULL COMMENT '头像url',
  `signature` varchar(128) DEFAULT NULL COMMENT '签名',
  `bio` text COMMENT '个人简介',
  `latitude` double DEFAULT NULL COMMENT '纬度',
  `longitude` double DEFAULT NULL COMMENT '经度',
  `like_count` int DEFAULT '0' COMMENT '点赞数',
  `post_count` int DEFAULT '0' COMMENT '发帖数',
  `created_at` datetime DEFAULT NULL COMMENT '注册时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `last_login_at` datetime DEFAULT NULL COMMENT '最后登录时间',
  `city` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '未知' COMMENT '城市名称',
  `is_bot` tinyint DEFAULT '0',
  `status` int DEFAULT '1' COMMENT '账号状态：1=正常 2=冻结',
  PRIMARY KEY (`user_id`) USING BTREE,
  KEY `idx_user_status` (`status`),
  KEY `idx_user_is_bot` (`is_bot`),
  KEY `idx_user_created_at` (`created_at`),
  KEY `idx_user_city` (`city`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for user_gallery
-- ----------------------------
DROP TABLE IF EXISTS `user_gallery`;
CREATE TABLE `user_gallery` (
  `id` varchar(255) NOT NULL,
  `user_id` varchar(255) NOT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `sort_order` int DEFAULT '0',
  `created_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for user_hobby
-- ----------------------------
DROP TABLE IF EXISTS `user_hobby`;
CREATE TABLE `user_hobby` (
  `id` varchar(255) NOT NULL,
  `user_id` varchar(255) NOT NULL,
  `hobby_name` varchar(32) NOT NULL,
  `icon` varchar(8) DEFAULT NULL,
  `create_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for user_skill
-- ----------------------------
DROP TABLE IF EXISTS `user_skill`;
CREATE TABLE `user_skill` (
  `id` varchar(36) NOT NULL,
  `user_Id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `skill_name` varchar(36) DEFAULT NULL COMMENT '技能名称',
  `skill_type` int DEFAULT NULL COMMENT '1-我会 2-想学',
  `created_time` datetime DEFAULT NULL COMMENT '创建技能时间',
  PRIMARY KEY (`id`,`user_Id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
