-- =============================================================
-- MySQL Schema for Node-Assigned Distributed Task System
-- Database: node_tasks
-- =============================================================

CREATE DATABASE IF NOT EXISTS `node_tasks`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `node_tasks`;

-- =============================================================
-- 1. NODE table — registered worker nodes
-- =============================================================
CREATE TABLE IF NOT EXISTS `node` (
  `id`                BIGINT        NOT NULL AUTO_INCREMENT,
  `node_id`           VARCHAR(255)  DEFAULT NULL,
  `created_on`        DATETIME(6)   DEFAULT NULL,
  `is_active`         BIT(1)        NOT NULL,
  `node_secret_hash`  VARCHAR(255)  DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- 2. TASK table — tasks assigned to nodes
-- =============================================================
CREATE TABLE IF NOT EXISTS `task` (
  `task_id`           VARCHAR(36)   NOT NULL,
  `task_type`         VARCHAR(255)  DEFAULT NULL,
  `task_details`      TEXT          DEFAULT NULL,
  `assigned_node_id`  VARCHAR(255)  DEFAULT NULL,
  `status`            ENUM('pending','in_progress','completed','failed','timed_out') DEFAULT 'pending',
  `created_on`        DATETIME(6)   DEFAULT NULL,
  `updated_on`        DATETIME(6)   DEFAULT NULL,
  PRIMARY KEY (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- 3. USER table — admin / API users
-- =============================================================
CREATE TABLE IF NOT EXISTS `user` (
  `id`                BIGINT        NOT NULL,
  `username`          VARCHAR(255)  DEFAULT NULL,
  `password_hash`     VARCHAR(255)  DEFAULT NULL,
  `active`            BIT(1)        NOT NULL,
  `role`              VARCHAR(255)  DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- NOTE: Hibernate auto-creates a `user_seq` table at runtime for
-- User ID generation (@GeneratedValue). No DDL needed here.


-- =============================================================
-- SAMPLE DATA (matches your current database)
-- =============================================================

-- Nodes (passwords are BCrypt hashes)
-- Node 1 & 4 share secret "node1Secret"
-- Node 2 & 3 share secret "node2Secret"
INSERT INTO `node` (`id`, `created_on`, `is_active`, `node_id`, `node_secret_hash`) VALUES
  (1, '2026-02-14 19:56:41.000000', b'1', '1', '$2a$10$h8X9S9zWkxw/z5S5F8Oa/OMz8F.R46nbwO8NH7iqNhbjp50H24VoW'),
  (2, '2026-02-14 19:56:41.000000', b'1', '2', '$2a$10$YAgQViKPxEBLnej36sEE.OjcIq97S7Uc6GOd4fSm3WEUvBzNtRP.'),
  (3, '2026-02-14 19:56:41.000000', b'1', '3', '$2a$10$YAgQViKPxEBLnej36sEE.OjcIq97S7Uc6GOd4fSm3WEUvBzNtRP.'),
  (4, '2026-02-14 19:56:41.000000', b'1', '4', '$2a$10$h8X9S9zWkxw/z5S5F8Oa/OMz8F.R46nbwO8NH7iqNhbjp50H24VoW')
ON DUPLICATE KEY UPDATE `id` = `id`;

-- Admin user (password: muopshi)
INSERT INTO `user` (`id`, `active`, `password_hash`, `role`, `username`) VALUES
  (1, b'1', '$2a$10$V7EQ3tOTF8rO9HrcGTRCFuWD1/r5m/ee2kqXbLsBq0Gyh/muopshi', 'admin', 'kartik.roy@explorisin')
ON DUPLICATE KEY UPDATE `id` = `id`;
