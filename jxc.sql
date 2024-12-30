/*
Navicat MySQL Data Transfer

Source Server         : local
Source Server Version : 80028
Source Host           : localhost:3306
Source Database       : jxc

Target Server Type    : MYSQL
Target Server Version : 80028
File Encoding         : 65001

Date: 2024-12-29 22:49:36
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for s_account_book
-- ----------------------------
DROP TABLE IF EXISTS `s_account_book`;
CREATE TABLE `s_account_book` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `checkout_date` date DEFAULT NULL,
  `current` bit(1) DEFAULT NULL,
  `enabled` bit(1) DEFAULT NULL,
  `merchant_id` bigint NOT NULL,
  `name` varchar(64) NOT NULL,
  `start_date` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKf3hqsnnsuubp2dp8m2xi3xr2` (`name`,`merchant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_account_book
-- ----------------------------
INSERT INTO `s_account_book` VALUES ('1', null, '\0', '', '1', '时逸（苏州）财税咨询有限责任公司', '2024-12-29');
INSERT INTO `s_account_book` VALUES ('2', null, '', '', '1', '纷析云（杭州）科技有限公司', '2024-12-29');

-- ----------------------------
-- Table structure for s_admin
-- ----------------------------
DROP TABLE IF EXISTS `s_admin`;
CREATE TABLE `s_admin` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '状态',
  `last_login_date` datetime(6) DEFAULT NULL COMMENT '最后登录时间',
  `merchant_id` bigint NOT NULL,
  `mobile` varchar(32) NOT NULL COMMENT '手机号码',
  `name` varchar(255) NOT NULL,
  `password` varchar(128) NOT NULL COMMENT '密码',
  `role_id` bigint DEFAULT NULL,
  `system_default` bit(1) DEFAULT NULL COMMENT '是否系统默认',
  `username` varchar(64) NOT NULL COMMENT '用户名',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK23bugxg955svtuy2j9d8wpqwu` (`mobile`,`merchant_id`),
  UNIQUE KEY `UK_hby25rpnay6vu2dgjpdxkn1lx` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_admin
-- ----------------------------
INSERT INTO `s_admin` VALUES ('1', '', null, '1', '13944878765', '李泽龙', '$2a$10$STj6SB0MPRAfqnOl2ZmV7O6YTC2R6UUY5ZhPQmyRgRYY9G3HTJWbK', '1', '', '13944878765');
INSERT INTO `s_admin` VALUES ('2', '', null, '2', '13456784500', 'abc', '$2a$10$vaw1OQkCCReeA2ZUL7XJKO1BddLRONZgPrGzw4oocvcfGtNaVT6zy', '2', '', '13456784500');

-- ----------------------------
-- Table structure for s_checkout
-- ----------------------------
DROP TABLE IF EXISTS `s_checkout`;
CREATE TABLE `s_checkout` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `check_date` date DEFAULT NULL,
  `check_id` bigint DEFAULT NULL COMMENT '结账人',
  `create_date` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `merchant_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKqpanwltshydnp8du0steoo29p` (`check_date`,`account_book_id`,`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_checkout
-- ----------------------------

-- ----------------------------
-- Table structure for s_customer
-- ----------------------------
DROP TABLE IF EXISTS `s_customer`;
CREATE TABLE `s_customer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `code` varchar(255) DEFAULT NULL,
  `contact_person` varchar(255) DEFAULT NULL,
  `customer_category_id` bigint NOT NULL,
  `customer_level_id` bigint NOT NULL,
  `merchant_id` bigint NOT NULL,
  `name` varchar(32) NOT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uc_customer_code` (`merchant_id`,`account_book_id`,`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_customer
-- ----------------------------

-- ----------------------------
-- Table structure for s_customer_category
-- ----------------------------
DROP TABLE IF EXISTS `s_customer_category`;
CREATE TABLE `s_customer_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `merchant_id` bigint NOT NULL,
  `name` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uc_customer_category_name` (`merchant_id`,`account_book_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_customer_category
-- ----------------------------

-- ----------------------------
-- Table structure for s_customer_level
-- ----------------------------
DROP TABLE IF EXISTS `s_customer_level`;
CREATE TABLE `s_customer_level` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `merchant_id` bigint NOT NULL,
  `name` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uc_customer_level_name` (`merchant_id`,`account_book_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_customer_level
-- ----------------------------

-- ----------------------------
-- Table structure for s_customer_level_price
-- ----------------------------
DROP TABLE IF EXISTS `s_customer_level_price`;
CREATE TABLE `s_customer_level_price` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `customer_level_id` bigint DEFAULT NULL,
  `merchant_id` bigint NOT NULL,
  `price` decimal(38,2) DEFAULT NULL COMMENT '基础单位价格',
  `product_id` bigint DEFAULT NULL,
  `unit_id` bigint DEFAULT NULL COMMENT '基础单位',
  `unit_price` json DEFAULT NULL COMMENT '客户单位辅助价格',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uc_customer_level_price` (`merchant_id`,`account_book_id`,`product_id`,`customer_level_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_customer_level_price
-- ----------------------------

-- ----------------------------
-- Table structure for s_menu
-- ----------------------------
DROP TABLE IF EXISTS `s_menu`;
CREATE TABLE `s_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `component` varchar(255) DEFAULT NULL COMMENT '组件',
  `enabled` bit(1) DEFAULT NULL COMMENT '是否启用',
  `icon_cls` varchar(255) DEFAULT NULL COMMENT '图标',
  `menu_group` varchar(255) DEFAULT NULL,
  `menu_module` varchar(32) DEFAULT 'MERCHANT',
  `menu_type` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL COMMENT '菜单名称',
  `parent_id` bigint DEFAULT NULL COMMENT '父id',
  `pos` int DEFAULT NULL COMMENT '位置',
  `require_auth` bit(1) DEFAULT NULL COMMENT '是否要求权限',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_menu
-- ----------------------------
INSERT INTO `s_menu` VALUES ('1', 'basic', '', 'h-icon-task', 'MERCHANT', 'MERCHANT', 'MENU', '基础资料', null, '0', '');
INSERT INTO `s_menu` VALUES ('6', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '基础资料', '1', '1', '');
INSERT INTO `s_menu` VALUES ('7', 'CustomerList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '客户', '6', '1', '');
INSERT INTO `s_menu` VALUES ('8', 'SupplierList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '供货商', '6', '2', '');
INSERT INTO `s_menu` VALUES ('9', 'WarehouseList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '仓库', '6', '3', '');
INSERT INTO `s_menu` VALUES ('10', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '辅助资料', '1', '2', '');
INSERT INTO `s_menu` VALUES ('12', 'CustomerLevelList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '客户等级', '10', '1', '');
INSERT INTO `s_menu` VALUES ('14', '', '', 'h-icon-setting', 'MERCHANT', 'MERCHANT', 'MENU', '系统设置', null, '10', '');
INSERT INTO `s_menu` VALUES ('15', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '系统设置', '14', '0', '');
INSERT INTO `s_menu` VALUES ('16', 'MerchantInfo', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '企业信息', '15', '0', '');
INSERT INTO `s_menu` VALUES ('17', 'AccountBookList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '账套管理', '15', '1', '');
INSERT INTO `s_menu` VALUES ('18', 'RoleList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '角色权限', '15', '2', '');
INSERT INTO `s_menu` VALUES ('19', 'AdminList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '员工账号', '15', '3', '');
INSERT INTO `s_menu` VALUES ('20', 'ProductList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '产品', '6', '0', '');
INSERT INTO `s_menu` VALUES ('21', 'UnitList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '计量单位', '10', '1', '');

-- ----------------------------
-- Table structure for s_menu_role
-- ----------------------------
DROP TABLE IF EXISTS `s_menu_role`;
CREATE TABLE `s_menu_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `menu_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_menu_role
-- ----------------------------

-- ----------------------------
-- Table structure for s_merchant
-- ----------------------------
DROP TABLE IF EXISTS `s_merchant`;
CREATE TABLE `s_merchant` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(255) DEFAULT NULL COMMENT '地址',
  `code` varchar(255) DEFAULT NULL COMMENT '商户编码',
  `create_date` datetime(6) DEFAULT NULL COMMENT '创建日期',
  `email` varchar(255) DEFAULT NULL COMMENT '邮箱',
  `enabled` bit(1) DEFAULT NULL COMMENT '是否启用',
  `linkman` varchar(255) DEFAULT NULL COMMENT '联系人',
  `mobile` varchar(255) DEFAULT NULL COMMENT '电话号码',
  `name` varchar(255) DEFAULT NULL COMMENT '商户名称',
  `service_end_date` date DEFAULT NULL COMMENT '服务结束时间',
  `service_start_date` date DEFAULT NULL COMMENT '服务开始时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_merchant
-- ----------------------------
INSERT INTO `s_merchant` VALUES ('1', '杭州', '32f90df1-c739-463b-8218-715437765807', '2024-12-29 18:54:06.747190', null, '', '李泽龙', '13944878765', '纷析云', null, null);

-- ----------------------------
-- Table structure for s_merchant_menu
-- ----------------------------
DROP TABLE IF EXISTS `s_merchant_menu`;
CREATE TABLE `s_merchant_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `menu_id` bigint NOT NULL,
  `merchant_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=40 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_merchant_menu
-- ----------------------------
INSERT INTO `s_merchant_menu` VALUES ('25', '1', '1');
INSERT INTO `s_merchant_menu` VALUES ('26', '6', '1');
INSERT INTO `s_merchant_menu` VALUES ('27', '7', '1');
INSERT INTO `s_merchant_menu` VALUES ('28', '8', '1');
INSERT INTO `s_merchant_menu` VALUES ('29', '9', '1');
INSERT INTO `s_merchant_menu` VALUES ('30', '10', '1');
INSERT INTO `s_merchant_menu` VALUES ('31', '12', '1');
INSERT INTO `s_merchant_menu` VALUES ('32', '14', '1');
INSERT INTO `s_merchant_menu` VALUES ('33', '15', '1');
INSERT INTO `s_merchant_menu` VALUES ('34', '16', '1');
INSERT INTO `s_merchant_menu` VALUES ('35', '17', '1');
INSERT INTO `s_merchant_menu` VALUES ('36', '18', '1');
INSERT INTO `s_merchant_menu` VALUES ('37', '19', '1');
INSERT INTO `s_merchant_menu` VALUES ('38', '20', '1');
INSERT INTO `s_merchant_menu` VALUES ('39', '21', '1');

-- ----------------------------
-- Table structure for s_merchant_user
-- ----------------------------
DROP TABLE IF EXISTS `s_merchant_user`;
CREATE TABLE `s_merchant_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `enabled` bit(1) DEFAULT NULL COMMENT '是否启用',
  `last_login_date` datetime(6) DEFAULT NULL COMMENT '最后登录时间',
  `name` varchar(255) NOT NULL,
  `password` varchar(128) NOT NULL COMMENT '密码',
  `system_default` bit(1) DEFAULT NULL COMMENT '是否系统默认',
  `username` varchar(32) NOT NULL COMMENT '用户名',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_o7sn18pnb5rxs77lrf47krklc` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_merchant_user
-- ----------------------------
INSERT INTO `s_merchant_user` VALUES ('1', '', '2024-12-29 21:27:50.647692', '李泽龙', '$2a$10$MPZ4oROTiWvlKqiq79Sk7.TMcY3D8p8edgUpO5UarsCMroUeChSQC', '', '13944878765');

-- ----------------------------
-- Table structure for s_product
-- ----------------------------
DROP TABLE IF EXISTS `s_product`;
CREATE TABLE `s_product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `category_id` bigint NOT NULL,
  `code` varchar(64) NOT NULL COMMENT '产品编码',
  `create_date` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `enable_multi_unit` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否启动辅助单位',
  `enabled` bit(1) DEFAULT NULL,
  `img_path` varchar(255) DEFAULT NULL COMMENT '商品图片',
  `merchant_id` bigint NOT NULL,
  `multi_unit` json DEFAULT NULL COMMENT '辅助单位',
  `name` varchar(64) NOT NULL COMMENT '名称',
  `pinyin` varchar(32) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `sort` int DEFAULT NULL COMMENT '排序',
  `specification` varchar(255) DEFAULT NULL,
  `unit_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uc_product_code` (`merchant_id`,`account_book_id`,`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_product
-- ----------------------------

-- ----------------------------
-- Table structure for s_product_category
-- ----------------------------
DROP TABLE IF EXISTS `s_product_category`;
CREATE TABLE `s_product_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `code` varchar(64) NOT NULL COMMENT '分类编码',
  `leaf` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否叶子节点',
  `merchant_id` bigint NOT NULL,
  `name` varchar(64) NOT NULL COMMENT '分类名称',
  `pid` bigint DEFAULT NULL COMMENT '父级ID',
  `sort` bigint DEFAULT NULL COMMENT '排序号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uc_product_category_code` (`merchant_id`,`account_book_id`,`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_product_category
-- ----------------------------

-- ----------------------------
-- Table structure for s_role
-- ----------------------------
DROP TABLE IF EXISTS `s_role`;
CREATE TABLE `s_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `merchant_id` bigint DEFAULT NULL,
  `name` varchar(32) NOT NULL,
  `system_default` bit(1) DEFAULT NULL COMMENT '是否系统默认',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK7y7m6gk2u5eabvlmvpbqdhkj9` (`name`,`merchant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_role
-- ----------------------------
INSERT INTO `s_role` VALUES ('1', '1', '商户管理员', '');
INSERT INTO `s_role` VALUES ('2', '2', '商户管理员', '');

-- ----------------------------
-- Table structure for s_supplier
-- ----------------------------
DROP TABLE IF EXISTS `s_supplier`;
CREATE TABLE `s_supplier` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `code` varchar(255) DEFAULT NULL,
  `contact_person` varchar(255) DEFAULT NULL COMMENT '联系人',
  `merchant_id` bigint NOT NULL,
  `name` varchar(32) NOT NULL,
  `phone` varchar(255) DEFAULT NULL COMMENT '电话',
  `supplier_category_id` bigint DEFAULT NULL COMMENT '货商分类ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uc_supplier_name` (`merchant_id`,`account_book_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_supplier
-- ----------------------------

-- ----------------------------
-- Table structure for s_supplier_category
-- ----------------------------
DROP TABLE IF EXISTS `s_supplier_category`;
CREATE TABLE `s_supplier_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `merchant_id` bigint NOT NULL,
  `name` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uc_supplierCategory_name` (`merchant_id`,`account_book_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_supplier_category
-- ----------------------------

-- ----------------------------
-- Table structure for s_unit
-- ----------------------------
DROP TABLE IF EXISTS `s_unit`;
CREATE TABLE `s_unit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `merchant_id` bigint NOT NULL,
  `name` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uc_unit_name` (`merchant_id`,`account_book_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_unit
-- ----------------------------

-- ----------------------------
-- Table structure for s_warehouse
-- ----------------------------
DROP TABLE IF EXISTS `s_warehouse`;
CREATE TABLE `s_warehouse` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `merchant_id` bigint NOT NULL,
  `name` varchar(32) NOT NULL,
  `system_default` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否默认',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uc_warehouse_name` (`merchant_id`,`account_book_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_warehouse
-- ----------------------------
