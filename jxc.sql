/*
Navicat MySQL Data Transfer

Source Server         : jxc线上测试库
Source Server Version : 80300
Source Host           : 139.159.132.75:3306
Source Database       : jxc

Target Server Type    : MYSQL
Target Server Version : 80300
File Encoding         : 65001

Date: 2025-01-06 12:56:05
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for jxc_account
-- ----------------------------
DROP TABLE IF EXISTS `jxc_account`;
CREATE TABLE `jxc_account`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `account_type` varchar(32) COLLATE utf8mb4_general_ci DEFAULT '银行存款' COMMENT '账户类别',
  `balance` decimal(38,2) DEFAULT NULL COMMENT '账户余额',
  `code` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '编码',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '状态',
  `merchant_id` bigint NOT NULL,
  `name` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `system_default` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否系统默认',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKe2l13ts6x87l689bwd3jm8g81` (`merchant_id`,`account_book_id`,`name`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_account
-- ----------------------------
INSERT INTO `jxc_account`
VALUES ('2', '1', '银行存款', null, '213', '', '1', '123', '');

-- ----------------------------
-- Table structure for jxc_account_book
-- ----------------------------
DROP TABLE IF EXISTS `jxc_account_book`;
CREATE TABLE `jxc_account_book`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `checkout_date` date DEFAULT NULL COMMENT '结账日期',
  `current` bit(1) DEFAULT NULL COMMENT '当前账套',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '状态',
  `merchant_id` bigint NOT NULL,
  `name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `start_date` date DEFAULT NULL COMMENT '启用日期',
  `warehouse_id` bigint DEFAULT NULL COMMENT '默认仓库',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKr68fd5d4eapkyrgnytwhglnjh` (`name`,`merchant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_account_book
-- ----------------------------
INSERT INTO `jxc_account_book`
VALUES ('1', null, '', '', '1', '纷析云', '2025-01-03', null);

-- ----------------------------
-- Table structure for jxc_account_flow
-- ----------------------------
DROP TABLE IF EXISTS `jxc_account_flow`;
CREATE TABLE `jxc_account_flow`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_flow_type` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '操作类型',
  `account_id` bigint NOT NULL COMMENT '账户Id',
  `amount` decimal(38,2) NOT NULL COMMENT '金额',
  `balance_after` decimal(38,2) DEFAULT NULL COMMENT '交易后余额',
  `balance_before` decimal(38,2) DEFAULT NULL COMMENT '交易前余额',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint                                  DEFAULT NULL COMMENT '创建人',
  `remarks`    varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `voucher_id` bigint DEFAULT NULL COMMENT '单据Id',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_account_flow
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_account_transfer
-- ----------------------------
DROP TABLE IF EXISTS `jxc_account_transfer`;
CREATE TABLE `jxc_account_transfer`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint                                 NOT NULL,
  `amount`          decimal(38, 2)                                  DEFAULT NULL COMMENT '金额',
  `approved_at`     datetime(6) DEFAULT NULL COMMENT '审核时间',
  `approved_by`     bigint                                          DEFAULT NULL COMMENT '审核人',
  `created_at`      datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by`      bigint                                          DEFAULT NULL COMMENT '创建人',
  `merchant_id`     bigint                                 NOT NULL,
  `order_date`      date                                            DEFAULT NULL COMMENT '单据日期',
  `order_no`        varchar(255) COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '单据编号',
  `order_status`    varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '已保存' COMMENT '状态',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_account_transfer
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_account_transfer_item
-- ----------------------------
DROP TABLE IF EXISTS `jxc_account_transfer_item`;
CREATE TABLE `jxc_account_transfer_item`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `amount`          decimal(38, 2) DEFAULT NULL COMMENT '金额',
  `from_account_id` bigint         DEFAULT NULL COMMENT '转出账户ID',
  `merchant_id`     bigint NOT NULL,
  `payment_method`  bigint         DEFAULT NULL COMMENT '结算方式',
  `to_account_id`   bigint         DEFAULT NULL COMMENT '转入账户ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_account_transfer_item
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_account_type
-- ----------------------------
DROP TABLE IF EXISTS `jxc_account_type`;
CREATE TABLE `jxc_account_type`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `cost_type` varchar(32) COLLATE utf8mb4_general_ci DEFAULT '收入' COMMENT '收支类别',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '状态',
  `merchant_id` bigint NOT NULL,
  `name` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `code` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKs0gpnbygsm5bj4o71qtinnkh2` (`merchant_id`,`account_book_id`,`name`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_account_type
-- ----------------------------
INSERT INTO `jxc_account_type`
VALUES ('3', '1', '收入类别', '', '1', '营业收入', '01');
INSERT INTO `jxc_account_type`
VALUES ('4', '1', '支出类别', '', '1', '管理费用', '02');

-- ----------------------------
-- Table structure for jxc_admin
-- ----------------------------
DROP TABLE IF EXISTS `jxc_admin`;
CREATE TABLE `jxc_admin`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '状态',
  `last_login_date` datetime(6) DEFAULT NULL COMMENT '最后登录时间',
  `merchant_id` bigint NOT NULL,
  `mobile`         varchar(32) COLLATE utf8mb4_general_ci  NOT NULL COMMENT '手机号码',
  `name`           varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '姓名',
  `password`       varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码',
  `role_id`        bigint                                           DEFAULT NULL COMMENT '角色ID',
  `system_default` bit(1)                                  NOT NULL DEFAULT b'1' COMMENT '是否系统默认',
  `username`       varchar(32) COLLATE utf8mb4_general_ci  NOT NULL COMMENT '用户名',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKt1i4pby0us6ny684d12nfyd4w` (`mobile`,`merchant_id`),
  UNIQUE KEY `UK_beb04mr7p4vie7do5i07ebplr` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_admin
-- ----------------------------
INSERT INTO `jxc_admin`
VALUES ('1', '', null, '1', '13944878765', '李泽龙', '$2a$10$iHhE0lp9QXbG4frpRtvm0uS2p1QzYwVRian01/v.bK1ZKU8xHApGW',
        '1', '', '13944878765');

-- ----------------------------
-- Table structure for jxc_checkout
-- ----------------------------
DROP TABLE IF EXISTS `jxc_checkout`;
CREATE TABLE `jxc_checkout`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `check_date` date DEFAULT NULL COMMENT '结账日期',
  `check_id` bigint DEFAULT NULL COMMENT '结账人',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `merchant_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK7iiih4ig7qjqi010ssmx4jjoq` (`check_date`,`account_book_id`,`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_checkout
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_code_rule
-- ----------------------------
DROP TABLE IF EXISTS `jxc_code_rule`;
CREATE TABLE `jxc_code_rule`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `date_format`  varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '日期格式',
  `document_type` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '单据类型',
  `merchant_id` bigint NOT NULL,
  `name`         varchar(32) COLLATE utf8mb4_general_ci  NOT NULL COMMENT '规则名称',
  `prefix`       varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '前缀',
  `reset_period` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '流水号清零',
  `serial_number_length` int DEFAULT NULL COMMENT '流水号位数',
  `start_value` int DEFAULT NULL COMMENT '起始值',
  `system_default` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否系统默认',
  `format`       varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '日期格式/分类编码',
  `created_at`   datetime(6) DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_code_rule
-- ----------------------------
INSERT INTO `jxc_code_rule`
VALUES ('1', '1', null, '采购订单', '1', '12312', '122121', '月', '1221', '12', '', '1212', null);

-- ----------------------------
-- Table structure for jxc_cost_adjustment
-- ----------------------------
DROP TABLE IF EXISTS `jxc_cost_adjustment`;
CREATE TABLE `jxc_cost_adjustment`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `adjustment_amount` decimal(38,2) DEFAULT NULL COMMENT '调整金额',
  `adjustment_type` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '入库调整' COMMENT '调整类型',
  `approved_at` datetime(6) DEFAULT NULL COMMENT '审核时间',
  `approved_by` bigint DEFAULT NULL COMMENT '审核人',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `djustment_date` datetime(6) DEFAULT NULL COMMENT '调整日期',
  `merchant_id` bigint NOT NULL,
  `order_no`     varchar(255) COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '单据编号',
  `order_status` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '已保存' COMMENT '订单状态',
  `remarks`      varchar(255) COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_cost_adjustment
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_cost_adjustment_item
-- ----------------------------
DROP TABLE IF EXISTS `jxc_cost_adjustment_item`;
CREATE TABLE `jxc_cost_adjustment_item`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `adjustment_amount` decimal(38,2) DEFAULT NULL COMMENT '调整金额：本次调整的总金额（调整数量 * (调整后成本 - 调整前成本)）',
  `base_unit_id` bigint DEFAULT NULL COMMENT '基本单位ID',
  `cost_adjustment_id` bigint DEFAULT NULL COMMENT '成本调整单主表',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `current_quantity` int                                     DEFAULT NULL COMMENT '库存数量',
  `merchant_id` bigint NOT NULL,
  `product_id` bigint DEFAULT NULL COMMENT '产品ID',
  `remarks`          varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `total_cost` decimal(38,2) DEFAULT NULL COMMENT '调整前的产品总成本',
  `updated_at`       datetime(6) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_cost_adjustment_item
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_customer
-- ----------------------------
DROP TABLE IF EXISTS `jxc_customer`;
CREATE TABLE `jxc_customer`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `balance` decimal(38, 2)                          DEFAULT NULL COMMENT '客户余额,应收账款',
  `code`    varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '编码',
  `contact` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系人',
  `customer_category_id` bigint NOT NULL COMMENT '客户分类ID',
  `customer_level_id` bigint NOT NULL COMMENT '客户等级ID',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '状态',
  `merchant_id` bigint NOT NULL,
  `name`    varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `phone`   varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '电话',
  `remarks` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKieny3rk8rej0613b2jues3xlv` (`merchant_id`,`account_book_id`,`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_customer
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_customer_category
-- ----------------------------
DROP TABLE IF EXISTS `jxc_customer_category`;
CREATE TABLE `jxc_customer_category`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `merchant_id` bigint NOT NULL,
  `name` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKdvyx480maig3pb4xktk5336qo` (`merchant_id`,`account_book_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_customer_category
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_customer_flow
-- ----------------------------
DROP TABLE IF EXISTS `jxc_customer_flow`;
CREATE TABLE `jxc_customer_flow`
(
    `id`                 bigint                                  NOT NULL AUTO_INCREMENT,
    `account_book_id`    bigint                                  NOT NULL,
    `amount`             decimal(38, 2)                          NOT NULL COMMENT '金额',
    `balance_after`      decimal(38, 2)                          DEFAULT NULL COMMENT '交易后余额',
    `balance_before`     decimal(38, 2)                          DEFAULT NULL COMMENT '交易前余额',
    `created_at`         datetime(6) DEFAULT NULL COMMENT '创建时间',
    `created_by`         bigint                                  DEFAULT NULL COMMENT '创建人',
    `customer_flow_type` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '操作类型',
    `customer_id`        bigint                                  NOT NULL COMMENT '客户Id',
    `is_initial`         bit(1)                                  DEFAULT NULL COMMENT '是否期初',
    `merchant_id`        bigint                                  NOT NULL,
    `order_id`           bigint                                  DEFAULT NULL COMMENT '单据Id',
    `remarks`            varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_customer_flow
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_customer_level
-- ----------------------------
DROP TABLE IF EXISTS `jxc_customer_level`;
CREATE TABLE `jxc_customer_level`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `merchant_id` bigint NOT NULL,
  `name` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKnphr8i1scthq0qyceovliuvxn` (`merchant_id`,`account_book_id`,`name`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_customer_level
-- ----------------------------
INSERT INTO `jxc_customer_level`
VALUES ('1', '1', '1', '客户等级');

-- ----------------------------
-- Table structure for jxc_customer_level_price
-- ----------------------------
DROP TABLE IF EXISTS `jxc_customer_level_price`;
CREATE TABLE `jxc_customer_level_price`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `auxiliary_unit_prices` json DEFAULT NULL COMMENT '客户单位辅助价格',
  `customer_level_id` bigint DEFAULT NULL COMMENT '客户等级ID',
  `merchant_id` bigint NOT NULL,
  `price` decimal(38,2) DEFAULT NULL COMMENT '基础单位价格',
  `product_id` bigint DEFAULT NULL COMMENT '产品ID',
  `unit_id` bigint DEFAULT NULL COMMENT '基础单位',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKf3jywf1k3jb4e1qc3dobrjpro` (`merchant_id`,`account_book_id`,`product_id`,`customer_level_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_customer_level_price
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_inventory
-- ----------------------------
DROP TABLE IF EXISTS `jxc_inventory`;
CREATE TABLE `jxc_inventory`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `average_cost` decimal(38,2) DEFAULT NULL COMMENT '平均成本',
  `base_unit_id` bigint DEFAULT NULL COMMENT '基础单位ID',
  `current_quantity` int DEFAULT NULL COMMENT '库存数量',
  `product_id` bigint DEFAULT NULL COMMENT '产品ID',
  `safety_stock` int DEFAULT NULL COMMENT '安全库存',
  `total_cost` decimal(38,2) DEFAULT NULL COMMENT '成本总计',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `account_book_id` bigint NOT NULL,
  `merchant_id`     bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_inventory
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_inventory_item
-- ----------------------------
DROP TABLE IF EXISTS `jxc_inventory_item`;
CREATE TABLE `jxc_inventory_item`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `base_unit_id` bigint DEFAULT NULL COMMENT '基础单位ID',
  `batch_number` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '批次号',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `merchant_id` bigint NOT NULL,
  `operation_type` smallint DEFAULT NULL COMMENT '操作类型：入库、出库、调拨',
  `order_id` bigint DEFAULT NULL COMMENT '关联单据ID',
  `product_id` bigint DEFAULT NULL COMMENT '产品ID',
  `quantity` int DEFAULT NULL COMMENT '正数为入库，负数为出库',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_inventory_item
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_inventory_transfer
-- ----------------------------
DROP TABLE IF EXISTS `jxc_inventory_transfer`;
CREATE TABLE `jxc_inventory_transfer`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `from_warehouse_id` bigint DEFAULT NULL COMMENT '调出仓库',
  `to_warehouse_id` bigint DEFAULT NULL COMMENT '调入仓库',
  `account_book_id` bigint NOT NULL,
  `approved_at` datetime(6) DEFAULT NULL COMMENT '审核时间',
  `approved_by` bigint DEFAULT NULL COMMENT '审核人',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `merchant_id` bigint NOT NULL,
  `order_no` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '单据编号',
  `order_status` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '已保存' COMMENT '订单状态',
  `remarks`  varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `transfer_date` datetime(6) DEFAULT NULL COMMENT '调拨日期',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_inventory_transfer
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_inventory_transfer_item
-- ----------------------------
DROP TABLE IF EXISTS `jxc_inventory_transfer_item`;
CREATE TABLE `jxc_inventory_transfer_item`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `from_warehouse_id` bigint DEFAULT NULL COMMENT '调出仓库ID',
  `inventory_transfer_id` bigint DEFAULT NULL COMMENT '调拨单主表ID',
  `merchant_id` bigint NOT NULL,
  `product_id` bigint DEFAULT NULL COMMENT '产品ID',
  `quantity` double DEFAULT NULL COMMENT '数量（以基本单位计）',
  `to_warehouse_id` bigint DEFAULT NULL COMMENT '调入仓库ID',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_inventory_transfer_item
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_menu
-- ----------------------------
DROP TABLE IF EXISTS `jxc_menu`;
CREATE TABLE `jxc_menu`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `component`   varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '组件',
  `enabled`     bit(1) NOT NULL                         DEFAULT b'1' COMMENT '状态',
  `icon_cls`    varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图标',
  `menu_group`  varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `menu_module` varchar(32) COLLATE utf8mb4_general_ci  DEFAULT 'MERCHANT',
  `menu_type`   varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `name`        varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '菜单名称',
  `parent_id` bigint DEFAULT NULL COMMENT '父id',
  `pos` int DEFAULT NULL COMMENT '位置',
  `require_auth` bit(1) DEFAULT NULL COMMENT '是否要求权限',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=87 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_menu
-- ----------------------------
INSERT INTO `jxc_menu`
VALUES ('1', 'basic', '', 'h-icon-task', 'MERCHANT', 'MERCHANT', 'MENU', '基础资料', null, '0', '');
INSERT INTO `jxc_menu`
VALUES ('6', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '基础资料', '1', '1', '');
INSERT INTO `jxc_menu`
VALUES ('7', 'CustomerList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '客户档案', '6', '1', '');
INSERT INTO `jxc_menu`
VALUES ('8', 'SupplierList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '货商档案', '6', '2', '');
INSERT INTO `jxc_menu`
VALUES ('9', 'WarehouseList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '仓库管理', '6', '3', '');
INSERT INTO `jxc_menu`
VALUES ('10', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '辅助资料', '1', '2', '');
INSERT INTO `jxc_menu`
VALUES ('12', 'CustomerLevelList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '客户等级', '10', '1', '');
INSERT INTO `jxc_menu`
VALUES ('14', 'Setting', '', 'h-icon-setting', 'MERCHANT', 'MERCHANT', 'MENU', '系统设置', null, '10', '');
INSERT INTO `jxc_menu`
VALUES ('15', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '系统设置', '14', '0', '');
INSERT INTO `jxc_menu`
VALUES ('16', 'MerchantInfo', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '企业信息', '15', '0', '');
INSERT INTO `jxc_menu`
VALUES ('17', 'AccountBookList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '账套管理', '15', '1', '');
INSERT INTO `jxc_menu`
VALUES ('18', 'RoleList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '角色权限', '15', '2', '');
INSERT INTO `jxc_menu`
VALUES ('19', 'AdminList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '员工账号', '15', '3', '');
INSERT INTO `jxc_menu`
VALUES ('20', 'ProductList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '产品档案', '6', '0', '');
INSERT INTO `jxc_menu`
VALUES ('23', 'AccountTypeList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '收支类型', '10', '3', '');
INSERT INTO `jxc_menu`
VALUES ('24', 'PaymentMethodList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '结算方式', '10', '4', '');
INSERT INTO `jxc_menu`
VALUES ('25', 'Purchase', '', 'h-icon-plus', 'MERCHANT', 'MERCHANT', 'MENU', '采购管理', null, '1', '');
INSERT INTO `jxc_menu`
VALUES ('26', 'Sales', '', 'h-icon-complete', 'MERCHANT', 'MERCHANT', 'MENU', '销售管理', null, '2', '');
INSERT INTO `jxc_menu`
VALUES ('27', 'Inventory', '', 'h-icon-check', 'MERCHANT', 'MERCHANT', 'MENU', '库存管理', null, '3', '');
INSERT INTO `jxc_menu`
VALUES ('28', 'Fund', '', 'h-icon-bell', 'MERCHANT', 'MERCHANT', 'MENU', '资金账户', null, '4', '');
INSERT INTO `jxc_menu`
VALUES ('29', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '资金单据', '28', '0', '');
INSERT INTO `jxc_menu`
VALUES ('30', 'OrderReceiptList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '收款单', '29', '0', '');
INSERT INTO `jxc_menu`
VALUES ('31', 'OrderPaymentList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '付款单', '29', '1', '');
INSERT INTO `jxc_menu`
VALUES ('32', 'VerificationList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '核销单', '29', '2', '');
INSERT INTO `jxc_menu`
VALUES ('33', 'AccountTransferList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '资金转账', '29', '3', '');
INSERT INTO `jxc_menu`
VALUES ('34', 'OtherIncomeList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '其他收入单', '29', '4', '');
INSERT INTO `jxc_menu`
VALUES ('35', 'OtherExpenseList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '其他支出单', '29', '5', '');
INSERT INTO `jxc_menu`
VALUES ('36', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '资金报表', '28', '1', '');
INSERT INTO `jxc_menu`
VALUES ('37', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '往来单位欠款表', '36', '0', '');
INSERT INTO `jxc_menu`
VALUES ('38', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '客户对账单', '36', '1', '');
INSERT INTO `jxc_menu`
VALUES ('39', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '供货商对账单', '36', '2', '');
INSERT INTO `jxc_menu`
VALUES ('40', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '应收账款明细表', '36', '3', '');
INSERT INTO `jxc_menu`
VALUES ('41', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '应付账款明细表', '36', '4', '');
INSERT INTO `jxc_menu`
VALUES ('42', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '其他收支明细表', '36', '5', '');
INSERT INTO `jxc_menu`
VALUES ('43', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '价格设置', '1', '3', '');
INSERT INTO `jxc_menu`
VALUES ('44', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '产品价格资料', '43', '0', '');
INSERT INTO `jxc_menu`
VALUES ('45', 'PriceRecordList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '产品价格记录', '43', '1', '');
INSERT INTO `jxc_menu`
VALUES ('46', 'CodeRuleList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '编码规则', '15', '4', '');
INSERT INTO `jxc_menu`
VALUES ('47', 'CheckoutList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '结账/反结账', '15', '5', '');
INSERT INTO `jxc_menu`
VALUES ('48', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '打印模板', '15', '6', '');
INSERT INTO `jxc_menu`
VALUES ('49', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '备份与恢复', '15', '7', '');
INSERT INTO `jxc_menu`
VALUES ('50', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '操作日志', '15', '8', '');
INSERT INTO `jxc_menu`
VALUES ('51', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '采购单据', '25', '0', '');
INSERT INTO `jxc_menu`
VALUES ('52', 'PurchaseOrderList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '采购订单', '51', '0', '');
INSERT INTO `jxc_menu`
VALUES ('53', 'PurchaseInboundList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '采购入库单', '51', '1', '');
INSERT INTO `jxc_menu`
VALUES ('54', 'PurchaseReturnList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '采购退货单', '51', '2', '');
INSERT INTO `jxc_menu`
VALUES ('55', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '采购报表', '25', '1', '');
INSERT INTO `jxc_menu`
VALUES ('56', 'PurchaseItemReport', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '采购明细表', '55', '0', '');
INSERT INTO `jxc_menu`
VALUES ('57', 'PurchaseSummaryReport', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '采购汇总表', '55', '1', '');
INSERT INTO `jxc_menu`
VALUES ('58', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '销售单据', '26', '0', '');
INSERT INTO `jxc_menu`
VALUES ('59', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '销售报表', '26', '1', '');
INSERT INTO `jxc_menu`
VALUES ('60', 'SalesOrderList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '销售订单', '58', '0', '');
INSERT INTO `jxc_menu`
VALUES ('61', 'SalesOutboundList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '销售出库单', '58', '1', '');
INSERT INTO `jxc_menu`
VALUES ('62', 'SalesReturnList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '销售退货单', '58', '2', '');
INSERT INTO `jxc_menu`
VALUES ('63', 'SalesItemReport', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '销售明细表', '59', '0', '');
INSERT INTO `jxc_menu`
VALUES ('64', 'SalesSummaryReport', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '销售汇总表', '59', '1', '');
INSERT INTO `jxc_menu`
VALUES ('65', 'SalesProfitReport', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '销售利润表', '59', '2', '');
INSERT INTO `jxc_menu`
VALUES ('66', 'SalesRankingReport', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '销售排行表', '59', '3', '');
INSERT INTO `jxc_menu`
VALUES ('67', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '库存单据', '27', '0', '');
INSERT INTO `jxc_menu`
VALUES ('68', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '库存报表', '27', '1', '');
INSERT INTO `jxc_menu`
VALUES ('72', 'InventoryReport', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '库存余额表', '68', '0', '');
INSERT INTO `jxc_menu`
VALUES ('73', 'InventoryItemReport', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '进销存明细表', '68', '1', '');
INSERT INTO `jxc_menu`
VALUES ('74', 'InventorySummaryReport', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '进销存汇总表', '68', '2', '');
INSERT INTO `jxc_menu`
VALUES ('75', 'InventoryTransferList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '调拨单', '67', '0', '');
INSERT INTO `jxc_menu`
VALUES ('76', 'StockTakeList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '盘点单', '67', '1', '');
INSERT INTO `jxc_menu`
VALUES ('77', 'OtherOutboundList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '其他出库单', '67', '2', '');
INSERT INTO `jxc_menu`
VALUES ('78', 'OtherInboundList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '其他入库单', '67', '3', '');
INSERT INTO `jxc_menu`
VALUES ('79', 'AccountList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '账户管理', '6', '4', '');
INSERT INTO `jxc_menu`
VALUES ('80', 'UnitList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '计量单位', '6', '6', '');
INSERT INTO `jxc_menu`
VALUES ('81', 'PricingPolicyList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '价格取数规则', '43', '2', '');
INSERT INTO `jxc_menu`
VALUES ('82', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '期初录入', '1', '4', '');
INSERT INTO `jxc_menu`
VALUES ('83', 'InventoryInitialList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '库存期初录入', '82', '0', '');
INSERT INTO `jxc_menu`
VALUES ('84', 'CustomerInitialList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '客户期初录入', '82', '1', '');
INSERT INTO `jxc_menu`
VALUES ('85', 'SupplierInitialList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '货商期初录入', '82', '2', '');
INSERT INTO `jxc_menu`
VALUES ('86', 'CostAdjustmentList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '成本调整单', '67', '5', '');

-- ----------------------------
-- Table structure for jxc_menu_role
-- ----------------------------
DROP TABLE IF EXISTS `jxc_menu_role`;
CREATE TABLE `jxc_menu_role`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `menu_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_menu_role
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_merchant
-- ----------------------------
DROP TABLE IF EXISTS `jxc_merchant`;
CREATE TABLE `jxc_merchant`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address`        varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '地址',
  `code`           varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '商户编码',
  `contact`        varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系人',
  `costing_method` smallint                                DEFAULT NULL COMMENT '存货计价方式',
  `created_at`     datetime(6) DEFAULT NULL COMMENT '创建日期',
  `email`          varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '邮箱',
  `enabled`        bit(1) NOT NULL                         DEFAULT b'1' COMMENT '状态',
  `mobile`         varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '电话号码',
  `name`           varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '商户名称',
  `service_end_date` date DEFAULT NULL COMMENT '服务结束时间',
  `service_start_date` date DEFAULT NULL COMMENT '服务开始时间',
  `stock_field` bit(1) DEFAULT NULL COMMENT '允许负库存',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_merchant
-- ----------------------------
INSERT INTO `jxc_merchant`
VALUES ('1', '杭州', '729ab676-bffa-4aba-9912-2370c54638d3', '李泽龙', null, '2025-01-03 21:44:10.058951', null, '',
        '13944878765', '纷析云', null, null, null);

-- ----------------------------
-- Table structure for jxc_merchant_menu
-- ----------------------------
DROP TABLE IF EXISTS `jxc_merchant_menu`;
CREATE TABLE `jxc_merchant_menu`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `menu_id` bigint NOT NULL,
  `merchant_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=78 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_merchant_menu
-- ----------------------------
INSERT INTO `jxc_merchant_menu`
VALUES ('1', '1', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('2', '6', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('3', '7', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('4', '8', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('5', '9', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('6', '10', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('7', '12', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('8', '14', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('9', '15', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('10', '16', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('11', '17', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('12', '18', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('13', '19', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('14', '20', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('15', '23', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('16', '24', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('17', '25', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('18', '26', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('19', '27', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('20', '28', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('21', '29', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('22', '30', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('23', '31', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('24', '32', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('25', '33', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('26', '34', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('27', '35', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('28', '36', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('29', '37', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('30', '38', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('31', '39', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('32', '40', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('33', '41', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('34', '42', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('35', '43', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('36', '44', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('37', '45', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('38', '46', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('39', '47', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('40', '48', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('41', '49', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('42', '50', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('43', '51', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('44', '52', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('45', '53', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('46', '54', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('47', '55', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('48', '56', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('49', '57', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('50', '58', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('51', '59', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('52', '60', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('53', '61', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('54', '62', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('55', '63', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('56', '64', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('57', '65', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('58', '66', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('59', '67', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('60', '68', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('64', '72', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('65', '73', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('66', '74', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('67', '75', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('68', '76', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('69', '77', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('70', '78', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('71', '79', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('72', '80', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('73', '81', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('74', '82', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('75', '83', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('76', '84', '1');
INSERT INTO `jxc_merchant_menu`
VALUES ('77', '85', '1');

-- ----------------------------
-- Table structure for jxc_merchant_user
-- ----------------------------
DROP TABLE IF EXISTS `jxc_merchant_user`;
CREATE TABLE `jxc_merchant_user`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `enabled`        bit(1)                                  NOT NULL DEFAULT b'1' COMMENT '状态',
  `last_login_date` datetime(6) DEFAULT NULL COMMENT '最后登录时间',
  `name`           varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '姓名',
  `password`       varchar(128) COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码',
  `system_default` bit(1)                                  NOT NULL DEFAULT b'1' COMMENT '是否系统默认',
  `username`       varchar(32) COLLATE utf8mb4_general_ci  NOT NULL COMMENT '用户名',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_fdtcg52m89b74wfc4p12ik1q4` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_merchant_user
-- ----------------------------
INSERT INTO `jxc_merchant_user`
VALUES ('1', '', '2025-01-03 21:47:52.361420', '李泽龙',
        '$2a$10$xdLpsVBapIN7wYcW8xpbFeg9kYKAV/UbDnr1zqC5uREe/ysbTpG1a', '', '13944878765');

-- ----------------------------
-- Table structure for jxc_order_payment
-- ----------------------------
DROP TABLE IF EXISTS `jxc_order_payment`;
CREATE TABLE `jxc_order_payment`
(
    `id`              bigint                                 NOT NULL AUTO_INCREMENT,
    `account_book_id` bigint                                 NOT NULL,
    `approved_at`     datetime(6) DEFAULT NULL COMMENT '审核时间',
    `approved_by`     bigint                                          DEFAULT NULL COMMENT '审核人',
    `created_at`      datetime(6) DEFAULT NULL COMMENT '创建时间',
    `created_by`      bigint                                          DEFAULT NULL COMMENT '创建人',
    `discount_amount` decimal(38, 2)                                  DEFAULT NULL COMMENT '折扣金额',
    `merchant_id`     bigint                                 NOT NULL,
    `order_date`      date                                            DEFAULT NULL COMMENT '单据日期',
    `order_no`        varchar(255) COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '单据编号',
    `order_status`    varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '已保存' COMMENT '状态',
    `payment_amount`  decimal(38, 2)                                  DEFAULT NULL COMMENT '付款金额',
    `supplier_id`     bigint                                          DEFAULT NULL COMMENT '货商ID',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_order_payment
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_order_payment_item
-- ----------------------------
DROP TABLE IF EXISTS `jxc_order_payment_item`;
CREATE TABLE `jxc_order_payment_item`
(
    `id`                       bigint                                  NOT NULL AUTO_INCREMENT,
    `one_time_verified_amount` decimal(38, 2)                          DEFAULT NULL COMMENT '本次核销金额',
    `order_amount`             decimal(38, 2)                          DEFAULT NULL COMMENT '单据金额',
    `order_date`               datetime(6) DEFAULT NULL COMMENT '单据日期',
    `order_id`                 bigint                                  DEFAULT NULL COMMENT '订单ID',
    `order_type`               varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务类型',
    `verified_amount`          decimal(38, 2)                          DEFAULT NULL COMMENT '已核销金额',
    `account_book_id`          bigint                                  NOT NULL,
    `account_id`               bigint                                  DEFAULT NULL COMMENT '账户ID',
    `amount`                   decimal(38, 2)                          DEFAULT NULL COMMENT '收入金额',
    `merchant_id`              bigint                                  NOT NULL,
    `payment_id`               bigint                                  DEFAULT NULL COMMENT '付款单ID',
    `payment_method_id`        bigint                                  DEFAULT NULL COMMENT '结算方式',
    `remarks`                  varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_order_payment_item
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_order_receipt
-- ----------------------------
DROP TABLE IF EXISTS `jxc_order_receipt`;
CREATE TABLE `jxc_order_receipt`
(
    `id`                bigint                                 NOT NULL AUTO_INCREMENT,
    `account_book_id`   bigint                                 NOT NULL,
    `approved_at`       datetime(6) DEFAULT NULL COMMENT '审核时间',
    `approved_by`       bigint                                          DEFAULT NULL COMMENT '审核人',
    `collection_amount` decimal(38, 2)                                  DEFAULT NULL COMMENT '收款金额',
    `created_at`        datetime(6) DEFAULT NULL COMMENT '创建时间',
    `created_by`        bigint                                          DEFAULT NULL COMMENT '创建人',
    `customer_id`       bigint                                          DEFAULT NULL COMMENT '客户ID',
    `discount_amount`   decimal(38, 2)                                  DEFAULT NULL COMMENT '折扣金额',
    `merchant_id`       bigint                                 NOT NULL,
    `order_date`        date                                            DEFAULT NULL COMMENT '单据日期',
    `order_no`          varchar(255) COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '单据编号',
    `order_status`      varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '已保存' COMMENT '状态',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_order_receipt
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_order_receipt_item
-- ----------------------------
DROP TABLE IF EXISTS `jxc_order_receipt_item`;
CREATE TABLE `jxc_order_receipt_item`
(
    `id`                       bigint                                  NOT NULL AUTO_INCREMENT,
    `one_time_verified_amount` decimal(38, 2)                          DEFAULT NULL COMMENT '本次核销金额',
    `order_amount`             decimal(38, 2)                          DEFAULT NULL COMMENT '单据金额',
    `order_date`               datetime(6) DEFAULT NULL COMMENT '单据日期',
    `order_id`                 bigint                                  DEFAULT NULL COMMENT '订单ID',
    `order_type`               varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务类型',
    `verified_amount`          decimal(38, 2)                          DEFAULT NULL COMMENT '已核销金额',
    `account_book_id`          bigint                                  NOT NULL,
    `account_id`               bigint                                  DEFAULT NULL COMMENT '账户ID',
    `amount`                   decimal(38, 2)                          DEFAULT NULL COMMENT '收入金额',
    `merchant_id`              bigint                                  NOT NULL,
    `payment_method_id`        bigint                                  DEFAULT NULL COMMENT '结算方式',
    `receipt_id`               bigint                                  DEFAULT NULL COMMENT '收款单ID',
    `remarks`                  varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_order_receipt_item
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_other_expense
-- ----------------------------
DROP TABLE IF EXISTS `jxc_other_expense`;
CREATE TABLE `jxc_other_expense`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `account_id`      bigint                                          DEFAULT NULL COMMENT '付款账户ID',
  `amount`          decimal(38, 2)                                  DEFAULT NULL COMMENT '金额',
  `approved_at` datetime(6) DEFAULT NULL COMMENT '审核时间',
  `approved_by` bigint DEFAULT NULL COMMENT '审核人',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `merchant_id` bigint NOT NULL,
  `order_date`      date                                            DEFAULT NULL COMMENT '单据日期',
  `order_no`        varchar(255) COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '单据编号',
  `order_status`    varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '已保存' COMMENT '状态',
  `payment_amount`  decimal(38, 2)                                  DEFAULT NULL COMMENT '付款金额',
  `supplier_id` bigint DEFAULT NULL COMMENT '供货商ID',
  `verified_amount` decimal(38, 2)                                  DEFAULT NULL COMMENT '已核销金额',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_other_expense
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_other_expense_item
-- ----------------------------
DROP TABLE IF EXISTS `jxc_other_expense_item`;
CREATE TABLE `jxc_other_expense_item`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `account_type_id`  bigint                                  DEFAULT NULL COMMENT '收支类别ID',
  `amount`           decimal(38, 2)                          DEFAULT NULL COMMENT '金额',
  `merchant_id` bigint NOT NULL,
  `other_expense_id` bigint                                  DEFAULT NULL COMMENT '其他支出单ID',
  `remarks`          varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_other_expense_item
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_other_inbound
-- ----------------------------
DROP TABLE IF EXISTS `jxc_other_inbound`;
CREATE TABLE `jxc_other_inbound`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `approved_at` datetime(6) DEFAULT NULL COMMENT '审核时间',
  `approved_by` bigint DEFAULT NULL COMMENT '审核人',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `discount_amount` decimal(38,2) DEFAULT NULL COMMENT '折扣金额',
  `final_amount` decimal(38,2) DEFAULT NULL COMMENT '折后金额',
  `inbound_date` datetime(6) DEFAULT NULL COMMENT '入库日期',
  `inbound_type`    varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '其他入库' COMMENT '入库类型',
  `merchant_id` bigint NOT NULL,
  `order_no`        varchar(255) COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '单据编号',
  `order_status`    varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '已保存' COMMENT '订单状态',
  `remarks`         varchar(255) COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '备注',
  `supplier_id` bigint DEFAULT NULL COMMENT '供货商ID',
  `total_amount` decimal(38,2) DEFAULT NULL COMMENT '订单金额',
  `verified_amount` decimal(38, 2)                                  DEFAULT NULL COMMENT '已核销金额，当货商ID不为空有效',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_other_inbound
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_other_inbound_item
-- ----------------------------
DROP TABLE IF EXISTS `jxc_other_inbound_item`;
CREATE TABLE `jxc_other_inbound_item`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `base_unit_id` bigint DEFAULT NULL COMMENT '基本单位ID',
  `batch_number`     varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '批次号',
  `conversion_rate` decimal(38,2) DEFAULT NULL COMMENT '换算率 (基本单位到辅助单位的换算率，例如：1箱=12个，则换算率为12。如果未使用辅助单位，则为1)',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `discount_rate` decimal(38,2) DEFAULT NULL COMMENT '折扣率',
  `discount_value` decimal(38,2) DEFAULT NULL COMMENT '折扣金额',
  `merchant_id` bigint NOT NULL,
  `other_inbound_id` bigint                                  DEFAULT NULL COMMENT '其他入库主表ID',
  `product_id` bigint DEFAULT NULL COMMENT '产品ID',
  `quantity` double DEFAULT NULL COMMENT '数量（以基本单位计）',
  `secondary_quantity` double DEFAULT NULL COMMENT '辅助单位数量 (可为空',
  `secondary_unit_id` bigint DEFAULT NULL COMMENT '辅助单位ID(可为空)',
  `subtotal` decimal(38,2) DEFAULT NULL COMMENT '小计 (quantity * unitPrice * (1 - discount_value/100) 或 quantity * unitPrice - discount_value，根据折扣类型计算)',
  `unit_price` decimal(38,2) DEFAULT NULL COMMENT '单价（以基本单位计）',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_other_inbound_item
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_other_income
-- ----------------------------
DROP TABLE IF EXISTS `jxc_other_income`;
CREATE TABLE `jxc_other_income`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id`   bigint                                 NOT NULL,
  `account_id`        bigint                                          DEFAULT NULL COMMENT '收款账户ID',
  `amount`            decimal(38, 2)                                  DEFAULT NULL COMMENT '金额',
  `approved_at`       datetime(6) DEFAULT NULL COMMENT '审核时间',
  `approved_by`       bigint                                          DEFAULT NULL COMMENT '审核人',
  `collection_amount` decimal(38, 2)                                  DEFAULT NULL COMMENT '收款金额',
  `created_at`        datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by`        bigint                                          DEFAULT NULL COMMENT '创建人',
  `customer_id`       bigint                                          DEFAULT NULL COMMENT '客户ID',
  `merchant_id`       bigint                                 NOT NULL,
  `order_date`        date                                            DEFAULT NULL COMMENT '单据日期',
  `order_no`          varchar(255) COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '单据编号',
  `order_status`      varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '已保存' COMMENT '状态',
  `verified_amount`   decimal(38, 2)                                  DEFAULT NULL COMMENT '已核销金额',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_other_income
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_other_income_item
-- ----------------------------
DROP TABLE IF EXISTS `jxc_other_income_item`;
CREATE TABLE `jxc_other_income_item`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `account_type_id` bigint                                  DEFAULT NULL COMMENT '收支类别ID',
  `amount`          decimal(38, 2)                          DEFAULT NULL COMMENT '收入金额',
  `merchant_id`     bigint NOT NULL,
  `other_income_id` bigint                                  DEFAULT NULL COMMENT '其他收入单ID',
  `remarks`         varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_other_income_item
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_other_outbound
-- ----------------------------
DROP TABLE IF EXISTS `jxc_other_outbound`;
CREATE TABLE `jxc_other_outbound`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint                                 NOT NULL,
  `approved_at`     datetime(6) DEFAULT NULL COMMENT '审核时间',
  `approved_by`     bigint                                          DEFAULT NULL COMMENT '审核人',
  `created_at`      datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by`      bigint                                          DEFAULT NULL COMMENT '创建人',
  `customer_id`     bigint                                          DEFAULT NULL COMMENT '客户ID',
  `discount_amount` decimal(38, 2)                                  DEFAULT NULL COMMENT '折扣金额',
  `final_amount`    decimal(38, 2)                                  DEFAULT NULL COMMENT '折后金额',
  `inbound_date`    datetime(6) DEFAULT NULL COMMENT '入库日期',
  `merchant_id`     bigint                                 NOT NULL,
  `order_no`        varchar(255) COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '单据编号',
  `order_status`    varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '已保存' COMMENT '订单状态',
  `outbound_type`   varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '其他出库' COMMENT '出库类型',
  `remarks`         varchar(255) COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '备注',
  `total_amount`    decimal(38, 2)                                  DEFAULT NULL COMMENT '订单金额',
  `verified_amount` decimal(38, 2)                                  DEFAULT NULL COMMENT '已核销金额，客户id不为空时有效',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_other_outbound
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_other_outbound_item
-- ----------------------------
DROP TABLE IF EXISTS `jxc_other_outbound_item`;
CREATE TABLE `jxc_other_outbound_item`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id`   bigint NOT NULL,
  `base_unit_id`      bigint                                  DEFAULT NULL COMMENT '基本单位ID',
  `batch_number`      varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '批次号',
  `conversion_rate`   decimal(38, 2)                          DEFAULT NULL COMMENT '换算率 (基本单位到辅助单位的换算率，例如：1箱=12个，则换算率为12。如果未使用辅助单位，则为1)',
  `created_at`        datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by`        bigint                                  DEFAULT NULL COMMENT '创建人',
  `discount_rate`     decimal(38, 2)                          DEFAULT NULL COMMENT '折扣率',
  `discount_value`    decimal(38, 2)                          DEFAULT NULL COMMENT '折扣金额',
  `merchant_id`       bigint NOT NULL,
  `other_outbound_id` bigint                                  DEFAULT NULL COMMENT '其他出库主表ID',
  `product_id`        bigint                                  DEFAULT NULL COMMENT '产品ID',
  `quantity` double DEFAULT NULL COMMENT '数量（以基本单位计）',
  `secondary_quantity` double DEFAULT NULL COMMENT '辅助单位数量 (可为空',
  `secondary_unit_id` bigint                                  DEFAULT NULL COMMENT '辅助单位ID(可为空)',
  `subtotal`          decimal(38, 2)                          DEFAULT NULL COMMENT '小计 (quantity * unitPrice * (1 - discount_value/100) 或 quantity * unitPrice - discount_value，根据折扣类型计算)',
  `unit_price`        decimal(38, 2)                          DEFAULT NULL COMMENT '单价（以基本单位计）',
  `updated_at`        datetime(6) DEFAULT NULL COMMENT '更新时间',
  `warehouse_id`      bigint                                  DEFAULT NULL COMMENT '仓库ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_other_outbound_item
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_payment
-- ----------------------------
DROP TABLE IF EXISTS `jxc_payment`;
CREATE TABLE `jxc_payment`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint                                 NOT NULL,
  `approved_at`     datetime(6) DEFAULT NULL COMMENT '审核时间',
  `approved_by`     bigint                                          DEFAULT NULL COMMENT '审核人',
  `created_at`      datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by`      bigint                                          DEFAULT NULL COMMENT '创建人',
  `discount_amount` decimal(38, 2)                                  DEFAULT NULL COMMENT '折扣金额',
  `merchant_id`     bigint                                 NOT NULL,
  `order_date` date DEFAULT NULL COMMENT '单据日期',
  `order_no`        varchar(255) COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '单据编号',
  `order_status`    varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '已保存' COMMENT '状态',
  `payment_amount`  decimal(38, 2)                                  DEFAULT NULL COMMENT '付款金额',
  `supplier_id`     bigint                                          DEFAULT NULL COMMENT '货商ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_payment
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_payment_item
-- ----------------------------
DROP TABLE IF EXISTS `jxc_payment_item`;
CREATE TABLE `jxc_payment_item`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id`   bigint NOT NULL,
  `account_id`        bigint                                  DEFAULT NULL COMMENT '账户ID',
  `amount`            decimal(38, 2)                          DEFAULT NULL COMMENT '收入金额',
  `merchant_id`       bigint NOT NULL,
  `payment_id`        bigint                                  DEFAULT NULL COMMENT '付款单ID',
  `payment_method_id` bigint                                  DEFAULT NULL COMMENT '结算方式',
  `remarks`           varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_payment_item
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_payment_method
-- ----------------------------
DROP TABLE IF EXISTS `jxc_payment_method`;
CREATE TABLE `jxc_payment_method`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `merchant_id` bigint NOT NULL,
  `name` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKosnhjd4mkycfpdywo2wxdnqy3` (`merchant_id`,`account_book_id`,`name`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_payment_method
-- ----------------------------
INSERT INTO `jxc_payment_method`
VALUES ('2', '1', '1', '微信');
INSERT INTO `jxc_payment_method`
VALUES ('3', '1', '1', '支付宝');
INSERT INTO `jxc_payment_method`
VALUES ('1', '1', '1', '银行转账');

-- ----------------------------
-- Table structure for jxc_price_record
-- ----------------------------
DROP TABLE IF EXISTS `jxc_price_record`;
CREATE TABLE `jxc_price_record`
(
    `id`                   bigint NOT NULL AUTO_INCREMENT,
    `account_book_id`      bigint NOT NULL,
    `base_unit_id`         bigint                                 DEFAULT NULL COMMENT '基本单位ID',
    `conversion_rate`      decimal(38, 2)                         DEFAULT NULL COMMENT '换算率 (基本单位到辅助单位的换算率，例如：1箱=12个，则换算率为12。如果未使用辅助单位，则为1)',
    `customer_id`          bigint                                 DEFAULT NULL COMMENT '客户ID',
    `merchant_id`          bigint NOT NULL,
    `order_date`           datetime(6) DEFAULT NULL COMMENT '单据日期',
    `order_id`             bigint                                 DEFAULT NULL COMMENT '单据主表ID',
    `price_type`           varchar(32) COLLATE utf8mb4_general_ci DEFAULT '采购入库' COMMENT '价格类别',
    `product_id`           bigint                                 DEFAULT NULL COMMENT '产品ID',
    `quantity` double DEFAULT NULL COMMENT '数量（以基本单位计）',
    `secondary_quantity` double DEFAULT NULL COMMENT '辅助单位数量 (可为空',
    `secondary_unit_id`    bigint                                 DEFAULT NULL COMMENT '辅助单位ID(可为空)',
    `secondary_unit_price` decimal(38, 2)                         DEFAULT NULL COMMENT '单价（辅助单位）',
    `supplier_id`          bigint                                 DEFAULT NULL COMMENT '货商ID',
    `unit_price`           decimal(38, 2)                         DEFAULT NULL COMMENT '单价（以基本单位计）',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_price_record
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_pricing_policy
-- ----------------------------
DROP TABLE IF EXISTS `jxc_pricing_policy`;
CREATE TABLE `jxc_pricing_policy`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '状态',
  `merchant_id` bigint NOT NULL,
  `price_source` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '价格来源',
  `price_type` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '价格类型',
  `priority` int DEFAULT NULL COMMENT '优先级',
  `remarks`    varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_pricing_policy
-- ----------------------------
INSERT INTO `jxc_pricing_policy`
VALUES ('1', '1', '', '1', '客户等级售价', '销售价格取数', '1', '取自商品的价格策略，根据客户等级设置价格');

-- ----------------------------
-- Table structure for jxc_product
-- ----------------------------
DROP TABLE IF EXISTS `jxc_product`;
CREATE TABLE `jxc_product`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `alert_quantity` int DEFAULT NULL COMMENT '预警库存',
  `auxiliary_unit_prices` json DEFAULT NULL COMMENT '辅助单位价格',
  `barcode`  varchar(32) COLLATE utf8mb4_general_ci  DEFAULT NULL COMMENT '条码',
  `code`     varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '编码',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `enable_auxiliary_unit` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否启用辅助单位',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '状态',
  `img_path` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '商品图片',
  `merchant_id` bigint NOT NULL,
  `name`     varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `pinyin`   varchar(32) COLLATE utf8mb4_general_ci  DEFAULT NULL COMMENT '拼音',
  `product_category_id` bigint NOT NULL COMMENT '产品分类',
  `purchase_price` decimal(38,2) NOT NULL COMMENT '预计进货价（基础单位）',
  `remarks`  varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `sort` int DEFAULT NULL COMMENT '排序',
  `specification` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '规格',
  `stock_quantity` int DEFAULT NULL COMMENT '库存数量',
  `unit_id` bigint NOT NULL COMMENT '基础单位',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKlpnfogmomnm7qqkuvq02ji9e` (`merchant_id`,`account_book_id`,`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_product
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_product_category
-- ----------------------------
DROP TABLE IF EXISTS `jxc_product_category`;
CREATE TABLE `jxc_product_category`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '分类编码',
  `leaf` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否叶子节点',
  `merchant_id` bigint NOT NULL,
  `name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '分类名称',
  `pid` bigint DEFAULT NULL COMMENT '父级ID',
  `sort` bigint DEFAULT NULL COMMENT '排序号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKe8q8337pf7e2ta56n7nh4jlql` (`merchant_id`,`account_book_id`,`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_product_category
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_purchase_inbound
-- ----------------------------
DROP TABLE IF EXISTS `jxc_purchase_inbound`;
CREATE TABLE `jxc_purchase_inbound`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `account_id`      bigint                                          DEFAULT NULL COMMENT '账户ID',
  `approved_at` datetime(6) DEFAULT NULL COMMENT '审核时间',
  `approved_by` bigint DEFAULT NULL COMMENT '审核人',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `discount_amount` decimal(38,2) DEFAULT NULL COMMENT '折扣金额',
  `final_amount` decimal(38,2) DEFAULT NULL COMMENT '折后金额',
  `inbound_date` datetime(6) DEFAULT NULL COMMENT '入库日期',
  `merchant_id` bigint NOT NULL,
  `order_id` bigint DEFAULT NULL COMMENT '采购订单ID',
  `order_no`        varchar(255) COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '单据编号',
  `order_status`    varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '已保存' COMMENT '订单状态',
  `payment_amount`  decimal(38, 2)                                  DEFAULT NULL COMMENT '付款金额',
  `remarks`         varchar(255) COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '备注',
  `supplier_id` bigint DEFAULT NULL COMMENT '供货商ID',
  `total_amount` decimal(38,2) DEFAULT NULL COMMENT '订单金额',
  `verified_amount` decimal(38, 2)                                  DEFAULT NULL COMMENT '已核销金额',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_purchase_inbound
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_purchase_inbound_item
-- ----------------------------
DROP TABLE IF EXISTS `jxc_purchase_inbound_item`;
CREATE TABLE `jxc_purchase_inbound_item`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `base_unit_id` bigint DEFAULT NULL COMMENT '基本单位ID',
  `batch_number` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '批次号',
  `conversion_rate` decimal(38,2) DEFAULT NULL COMMENT '换算率 (基本单位到辅助单位的换算率，例如：1箱=12个，则换算率为12。如果未使用辅助单位，则为1)',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `discount_rate` decimal(38,2) DEFAULT NULL COMMENT '折扣率',
  `discount_value` decimal(38,2) DEFAULT NULL COMMENT '折扣金额',
  `merchant_id` bigint NOT NULL,
  `product_id` bigint DEFAULT NULL COMMENT '产品ID',
  `purchase_inbound_id` bigint DEFAULT NULL COMMENT '采购入库主表ID',
  `quantity` double DEFAULT NULL COMMENT '数量（以基本单位计）',
  `secondary_quantity` double DEFAULT NULL COMMENT '辅助单位数量 (可为空',
  `secondary_unit_id` bigint DEFAULT NULL COMMENT '辅助单位ID(可为空)',
  `subtotal` decimal(38, 2) DEFAULT NULL COMMENT '小计 (quantity * unitPrice * (1 - discount_value/100) 或 quantity * unitPrice - discount_value，根据折扣类型计算)',
  `unit_price` decimal(38,2) DEFAULT NULL COMMENT '单价（以基本单位计）',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_purchase_inbound_item
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_purchase_order
-- ----------------------------
DROP TABLE IF EXISTS `jxc_purchase_order`;
CREATE TABLE `jxc_purchase_order`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `approved_at` datetime(6) DEFAULT NULL COMMENT '审核时间',
  `approved_by` bigint DEFAULT NULL COMMENT '审核人',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `discount_amount` decimal(38,2) DEFAULT NULL COMMENT '折扣金额',
  `final_amount` decimal(38,2) DEFAULT NULL COMMENT '折后金额',
  `merchant_id` bigint NOT NULL,
  `order_date` date DEFAULT NULL COMMENT '下单日期',
  `order_no` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '单据编号',
  `order_status` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '已保存' COMMENT '订单状态',
  `remarks`  varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `supplier_id` bigint DEFAULT NULL COMMENT '供货商ID',
  `total_amount` decimal(38,2) DEFAULT NULL COMMENT '订单金额',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_purchase_order
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_purchase_order_item
-- ----------------------------
DROP TABLE IF EXISTS `jxc_purchase_order_item`;
CREATE TABLE `jxc_purchase_order_item`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `base_unit_id` bigint DEFAULT NULL COMMENT '基本单位ID',
  `conversion_rate` decimal(38,2) DEFAULT NULL COMMENT '换算率 (基本单位到辅助单位的换算率，例如：1箱=12个，则换算率为12。如果未使用辅助单位，则为1)',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `discount_rate` decimal(38,2) DEFAULT NULL COMMENT '折扣率',
  `discount_value` decimal(38,2) DEFAULT NULL COMMENT '折扣金额',
  `merchant_id` bigint NOT NULL,
  `product_id` bigint DEFAULT NULL COMMENT '产品ID',
  `purchase_order_id` bigint DEFAULT NULL COMMENT '采购订单主表ID',
  `quantity` double DEFAULT NULL COMMENT '数量（以基本单位计）',
  `secondary_quantity` double DEFAULT NULL COMMENT '辅助单位数量 (可为空',
  `secondary_unit_id` bigint DEFAULT NULL COMMENT '辅助单位ID(可为空)',
  `subtotal` decimal(38, 2) DEFAULT NULL COMMENT '小计 (quantity * unitPrice * (1 - discount_value/100) 或 quantity * unitPrice - discount_value，根据折扣类型计算)',
  `unit_price` decimal(38,2) DEFAULT NULL COMMENT '单价（以基本单位计）',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_purchase_order_item
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_purchase_price_record
-- ----------------------------
DROP TABLE IF EXISTS `jxc_purchase_price_record`;
CREATE TABLE `jxc_purchase_price_record`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `base_unit_id` bigint DEFAULT NULL COMMENT '基本单位ID',
  `conversion_rate` decimal(38,2) DEFAULT NULL COMMENT '换算率 (基本单位到辅助单位的换算率，例如：1箱=12个，则换算率为12。如果未使用辅助单位，则为1)',
  `merchant_id` bigint NOT NULL,
  `product_id` bigint DEFAULT NULL COMMENT '产品ID',
  `purchase_date` datetime(6) DEFAULT NULL COMMENT '采购日期',
  `purchase_inbound_id` bigint DEFAULT NULL COMMENT '采购入库主表ID',
  `quantity` double DEFAULT NULL COMMENT '数量（以基本单位计）',
  `secondary_quantity` double DEFAULT NULL COMMENT '辅助单位数量 (可为空',
  `secondary_unit_id` bigint DEFAULT NULL COMMENT '辅助单位ID(可为空)',
  `secondary_unit_price` decimal(38,2) DEFAULT NULL COMMENT '单价（辅助单位）',
  `supplier_id` bigint DEFAULT NULL COMMENT '货商ID',
  `unit_price` decimal(38,2) DEFAULT NULL COMMENT '单价（以基本单位计）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_purchase_price_record
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_purchase_return
-- ----------------------------
DROP TABLE IF EXISTS `jxc_purchase_return`;
CREATE TABLE `jxc_purchase_return`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `account_id`        bigint                                          DEFAULT NULL COMMENT '账户ID',
  `approved_at` datetime(6) DEFAULT NULL COMMENT '审核时间',
  `approved_by` bigint DEFAULT NULL COMMENT '审核人',
  `collection_amount` decimal(38, 2)                                  DEFAULT NULL COMMENT '收款金额',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `merchant_id` bigint NOT NULL,
  `order_no`          varchar(255) COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '单据编号',
  `order_status`      varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '已保存' COMMENT '订单状态',
  `purchase_inbound_id` bigint DEFAULT NULL COMMENT '采购入库主表ID',
  `refund_amount` decimal(38,2) DEFAULT NULL COMMENT '退款金额',
  `remarks`           varchar(255) COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '备注',
  `return_date` date DEFAULT NULL COMMENT '退单日期',
  `return_reason`     varchar(255) COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '退单原因',
  `supplier_id` bigint DEFAULT NULL COMMENT '供货商ID',
  `verified_amount`   decimal(38, 2)                                  DEFAULT NULL COMMENT '已核销金额',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_purchase_return
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_purchase_return_item
-- ----------------------------
DROP TABLE IF EXISTS `jxc_purchase_return_item`;
CREATE TABLE `jxc_purchase_return_item`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `base_unit_id` bigint DEFAULT NULL COMMENT '基本单位ID',
  `batch_number` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '批次号',
  `conversion_rate` decimal(38,2) DEFAULT NULL COMMENT '换算率 (基本单位到辅助单位的换算率，例如：1箱=12个，则换算率为12。如果未使用辅助单位，则为1)',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `discount_rate` decimal(38,2) DEFAULT NULL COMMENT '折扣率',
  `discount_value` decimal(38,2) DEFAULT NULL COMMENT '折扣金额',
  `merchant_id` bigint NOT NULL,
  `product_id` bigint DEFAULT NULL COMMENT '产品ID',
  `purchase_return_id` bigint DEFAULT NULL COMMENT '采购入库单ID',
  `quantity` double DEFAULT NULL COMMENT '数量（以基本单位计）',
  `return_reason` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '退货原因',
  `secondary_quantity` double DEFAULT NULL COMMENT '辅助单位数量 (可为空',
  `secondary_unit_id` bigint DEFAULT NULL COMMENT '辅助单位ID(可为空)',
  `subtotal`     decimal(38, 2)                          DEFAULT NULL COMMENT '小计 (quantity * unitPrice * (1 - discount_value/100) 或 quantity * unitPrice - discount_value，根据折扣类型计算)',
  `unit_price` decimal(38,2) DEFAULT NULL COMMENT '单价（以基本单位计）',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_purchase_return_item
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_receipt
-- ----------------------------
DROP TABLE IF EXISTS `jxc_receipt`;
CREATE TABLE `jxc_receipt`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id`   bigint                                 NOT NULL,
  `approved_at`       datetime(6) DEFAULT NULL COMMENT '审核时间',
  `approved_by`       bigint                                          DEFAULT NULL COMMENT '审核人',
  `collection_amount` decimal(38, 2)                                  DEFAULT NULL COMMENT '收款金额',
  `created_at`        datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by`        bigint                                          DEFAULT NULL COMMENT '创建人',
  `customer_id`       bigint                                          DEFAULT NULL COMMENT '客户ID',
  `discount_amount`   decimal(38, 2)                                  DEFAULT NULL COMMENT '折扣金额',
  `merchant_id`       bigint                                 NOT NULL,
  `order_date` date DEFAULT NULL COMMENT '单据日期',
  `order_no`          varchar(255) COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '单据编号',
  `order_status`      varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '已保存' COMMENT '状态',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_receipt
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_receipt_item
-- ----------------------------
DROP TABLE IF EXISTS `jxc_receipt_item`;
CREATE TABLE `jxc_receipt_item`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id`   bigint NOT NULL,
  `account_id`        bigint                                  DEFAULT NULL COMMENT '账户ID',
  `amount`            decimal(38, 2)                          DEFAULT NULL COMMENT '收入金额',
  `merchant_id`       bigint NOT NULL,
  `payment_method_id` bigint                                  DEFAULT NULL COMMENT '结算方式',
  `receipt_id`        bigint                                  DEFAULT NULL COMMENT '收款单ID',
  `remarks`           varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_receipt_item
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_role
-- ----------------------------
DROP TABLE IF EXISTS `jxc_role`;
CREATE TABLE `jxc_role`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `merchant_id` bigint DEFAULT NULL,
  `name`           varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `system_default` bit(1)                                 NOT NULL DEFAULT b'1' COMMENT '是否系统默认',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKj3j714xt1b8ocx4vh54owfipk` (`name`,`merchant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_role
-- ----------------------------
INSERT INTO `jxc_role`
VALUES ('1', '1', '商户管理员', '');

-- ----------------------------
-- Table structure for jxc_sales_order
-- ----------------------------
DROP TABLE IF EXISTS `jxc_sales_order`;
CREATE TABLE `jxc_sales_order`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `approved_at` datetime(6) DEFAULT NULL COMMENT '审核时间',
  `approved_by` bigint DEFAULT NULL COMMENT '审核人',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `customer_id` bigint DEFAULT NULL COMMENT '客户ID',
  `discount_amount` decimal(38,2) DEFAULT NULL COMMENT '折扣金额',
  `final_amount` decimal(38,2) DEFAULT NULL COMMENT '折后金额',
  `merchant_id` bigint NOT NULL,
  `order_date` date DEFAULT NULL COMMENT '下单日期',
  `order_no` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '单据编号',
  `order_status` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '已保存' COMMENT '订单状态',
  `remarks`  varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `total_amount` decimal(38,2) DEFAULT NULL COMMENT '订单金额',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_sales_order
-- ----------------------------
INSERT INTO `jxc_sales_order`
VALUES ('1', '1', '2025-01-05 21:53:43.000000', '1', '2025-01-05 21:53:50.000000', '1', '1', '20.00', '200.00', '1',
        '2025-01-05', '21554877', '已保存', null, '220.00');

-- ----------------------------
-- Table structure for jxc_sales_order_item
-- ----------------------------
DROP TABLE IF EXISTS `jxc_sales_order_item`;
CREATE TABLE `jxc_sales_order_item`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `base_unit_id` bigint DEFAULT NULL COMMENT '基本单位ID',
  `conversion_rate` decimal(38,2) DEFAULT NULL COMMENT '换算率 (基本单位到辅助单位的换算率，例如：1箱=12个，则换算率为12。如果未使用辅助单位，则为1)',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `discount_rate` decimal(38,2) DEFAULT NULL COMMENT '折扣率',
  `discount_value` decimal(38,2) DEFAULT NULL COMMENT '折扣金额',
  `merchant_id` bigint NOT NULL,
  `product_id` bigint DEFAULT NULL COMMENT '产品ID',
  `quantity` double DEFAULT NULL COMMENT '数量（以基本单位计）',
  `sales_order_id` bigint DEFAULT NULL COMMENT '销售订单主表ID',
  `secondary_quantity` double DEFAULT NULL COMMENT '辅助单位数量 (可为空',
  `secondary_unit_id` bigint DEFAULT NULL COMMENT '辅助单位ID(可为空)',
  `subtotal` decimal(38,2) DEFAULT NULL COMMENT '小计 (quantity * unitPrice * (1 - discount_value/100) 或 quantity * unitPrice - discount_value，根据折扣类型计算)',
  `unit_price` decimal(38,2) DEFAULT NULL COMMENT '单价（以基本单位计）',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_sales_order_item
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_sales_outbound
-- ----------------------------
DROP TABLE IF EXISTS `jxc_sales_outbound`;
CREATE TABLE `jxc_sales_outbound`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `account_id`        bigint                                          DEFAULT NULL COMMENT '账户ID',
  `approved_at` datetime(6) DEFAULT NULL COMMENT '审核时间',
  `approved_by` bigint DEFAULT NULL COMMENT '审核人',
  `collection_amount` decimal(38, 2)                                  DEFAULT NULL COMMENT '收款金额',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `customer_id` bigint DEFAULT NULL COMMENT '客户ID',
  `discount_amount` decimal(38,2) DEFAULT NULL COMMENT '折扣金额',
  `final_amount` decimal(38,2) DEFAULT NULL COMMENT '折后金额',
  `merchant_id` bigint NOT NULL,
  `order_id` bigint DEFAULT NULL COMMENT '销售订单ID',
  `order_no`          varchar(255) COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '单据编号',
  `order_status`      varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '已保存' COMMENT '订单状态',
  `outbound_date` datetime(6) DEFAULT NULL COMMENT '出库日期',
  `remarks`           varchar(255) COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '备注',
  `total_amount` decimal(38,2) DEFAULT NULL COMMENT '订单金额',
  `verified_amount`   decimal(38, 2)                                  DEFAULT NULL COMMENT '已核销金额',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_sales_outbound
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_sales_outbound_item
-- ----------------------------
DROP TABLE IF EXISTS `jxc_sales_outbound_item`;
CREATE TABLE `jxc_sales_outbound_item`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `base_unit_id` bigint DEFAULT NULL COMMENT '基本单位ID',
  `conversion_rate` decimal(38,2) DEFAULT NULL COMMENT '换算率 (基本单位到辅助单位的换算率，例如：1箱=12个，则换算率为12。如果未使用辅助单位，则为1)',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `discount_rate` decimal(38,2) DEFAULT NULL COMMENT '折扣率',
  `discount_value` decimal(38,2) DEFAULT NULL COMMENT '折扣金额',
  `merchant_id` bigint NOT NULL,
  `product_id` bigint DEFAULT NULL COMMENT '产品ID',
  `quantity` double DEFAULT NULL COMMENT '数量（以基本单位计）',
  `sales_outbound_id` bigint DEFAULT NULL COMMENT '销售出库主表ID',
  `secondary_quantity` double DEFAULT NULL COMMENT '辅助单位数量 (可为空',
  `secondary_unit_id` bigint DEFAULT NULL COMMENT '辅助单位ID(可为空)',
  `subtotal` decimal(38,2) DEFAULT NULL COMMENT '小计 (quantity * unitPrice * (1 - discount_value/100) 或 quantity * unitPrice - discount_value，根据折扣类型计算)',
  `unit_price` decimal(38,2) DEFAULT NULL COMMENT '单价（以基本单位计）',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_sales_outbound_item
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_sales_price_record
-- ----------------------------
DROP TABLE IF EXISTS `jxc_sales_price_record`;
CREATE TABLE `jxc_sales_price_record`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `base_unit_id` bigint DEFAULT NULL COMMENT '基本单位ID',
  `conversion_rate` decimal(38,2) DEFAULT NULL COMMENT '换算率 (基本单位到辅助单位的换算率，例如：1箱=12个，则换算率为12。如果未使用辅助单位，则为1)',
  `customer_id` bigint DEFAULT NULL COMMENT '客户ID',
  `merchant_id` bigint NOT NULL,
  `product_id` bigint DEFAULT NULL COMMENT '产品ID',
  `quantity` double DEFAULT NULL COMMENT '数量（以基本单位计）',
  `sales_date` datetime(6) DEFAULT NULL COMMENT '销售日期',
  `sales_outbound_id` bigint DEFAULT NULL COMMENT '销售出库主表ID',
  `secondary_quantity` double DEFAULT NULL COMMENT '辅助单位数量 (可为空',
  `secondary_unit_id` bigint DEFAULT NULL COMMENT '辅助单位ID(可为空)',
  `secondary_unit_price` decimal(38,2) DEFAULT NULL COMMENT '单价（辅助单位）',
  `unit_price` decimal(38,2) DEFAULT NULL COMMENT '单价（以基本单位计）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_sales_price_record
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_sales_return
-- ----------------------------
DROP TABLE IF EXISTS `jxc_sales_return`;
CREATE TABLE `jxc_sales_return`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `account_id`      bigint                                          DEFAULT NULL COMMENT '账户ID',
  `approved_at` datetime(6) DEFAULT NULL COMMENT '审核时间',
  `approved_by` bigint DEFAULT NULL COMMENT '审核人',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `customer_id` bigint DEFAULT NULL COMMENT '客户ID',
  `merchant_id` bigint NOT NULL,
  `order_no`        varchar(255) COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '单据编号',
  `order_status`    varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '已保存' COMMENT '订单状态',
  `payment_amount`  decimal(38, 2)                                  DEFAULT NULL COMMENT '付款金额',
  `refund_amount` decimal(38,2) DEFAULT NULL COMMENT '退款金额',
  `remarks`         varchar(255) COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '备注',
  `return_date` date DEFAULT NULL COMMENT '退单日期',
  `return_reason`   varchar(255) COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '退单原因',
  `sales_outbound_id` bigint DEFAULT NULL COMMENT '销售出库主表ID',
  `verified_amount` decimal(38, 2)                                  DEFAULT NULL COMMENT '已核销金额',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_sales_return
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_sales_return_item
-- ----------------------------
DROP TABLE IF EXISTS `jxc_sales_return_item`;
CREATE TABLE `jxc_sales_return_item`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `base_unit_id` bigint DEFAULT NULL COMMENT '基本单位ID',
  `conversion_rate` decimal(38,2) DEFAULT NULL COMMENT '换算率 (基本单位到辅助单位的换算率，例如：1箱=12个，则换算率为12。如果未使用辅助单位，则为1)',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `discount_rate` decimal(38,2) DEFAULT NULL COMMENT '折扣率',
  `discount_value` decimal(38,2) DEFAULT NULL COMMENT '折扣金额',
  `merchant_id` bigint NOT NULL,
  `product_id` bigint DEFAULT NULL COMMENT '产品ID',
  `quantity` double DEFAULT NULL COMMENT '数量（以基本单位计）',
  `return_reason` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '退货原因',
  `sales_return_id` bigint DEFAULT NULL COMMENT '销售退单主表ID',
  `secondary_quantity` double DEFAULT NULL COMMENT '辅助单位数量 (可为空',
  `secondary_unit_id` bigint DEFAULT NULL COMMENT '辅助单位ID(可为空)',
  `subtotal` decimal(38,2) DEFAULT NULL COMMENT '小计 (quantity * unitPrice * (1 - discount_value/100) 或 quantity * unitPrice - discount_value，根据折扣类型计算)',
  `unit_price` decimal(38,2) DEFAULT NULL COMMENT '单价（以基本单位计）',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_sales_return_item
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_stock_take
-- ----------------------------
DROP TABLE IF EXISTS `jxc_stock_take`;
CREATE TABLE `jxc_stock_take`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `approved_at` datetime(6) DEFAULT NULL COMMENT '审核时间',
  `approved_by` bigint DEFAULT NULL COMMENT '审核人',
  `check_date` datetime(6) DEFAULT NULL COMMENT '盘点日期',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `merchant_id` bigint NOT NULL,
  `order_no` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '单据编号',
  `order_status` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '已保存' COMMENT '订单状态',
  `remarks`  varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_stock_take
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_stock_take_item
-- ----------------------------
DROP TABLE IF EXISTS `jxc_stock_take_item`;
CREATE TABLE `jxc_stock_take_item`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `stock_take_id` bigint DEFAULT NULL COMMENT '盘点主表ID',
  `account_book_id` bigint NOT NULL,
  `actual_quantity` int DEFAULT NULL COMMENT '实盘数量',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `difference_reason` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `merchant_id` bigint NOT NULL,
  `product_id` bigint DEFAULT NULL COMMENT '产品ID',
  `system_quantity` int DEFAULT NULL COMMENT '账面库存',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_stock_take_item
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_supplier
-- ----------------------------
DROP TABLE IF EXISTS `jxc_supplier`;
CREATE TABLE `jxc_supplier`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `balance` decimal(38, 2)                                  DEFAULT NULL COMMENT '货商余额,应付账款',
  `code`    varchar(255) COLLATE utf8mb4_general_ci         DEFAULT NULL,
  `contact` varchar(255) COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '联系人',
  `enabled` bit(1)                                 NOT NULL DEFAULT b'1' COMMENT '状态',
  `merchant_id` bigint NOT NULL,
  `name`    varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `phone`   varchar(255) COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '电话',
  `supplier_category_id` bigint DEFAULT NULL COMMENT '货商分类ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK3qa8vhhsefuy4lokphwovdwxc` (`merchant_id`,`account_book_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_supplier
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_supplier_category
-- ----------------------------
DROP TABLE IF EXISTS `jxc_supplier_category`;
CREATE TABLE `jxc_supplier_category`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `merchant_id` bigint NOT NULL,
  `name` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK9cuof0k46uxastjsi8eh37irq` (`merchant_id`,`account_book_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_supplier_category
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_supplier_flow
-- ----------------------------
DROP TABLE IF EXISTS `jxc_supplier_flow`;
CREATE TABLE `jxc_supplier_flow`
(
    `id`                 bigint                                  NOT NULL AUTO_INCREMENT,
    `account_book_id`    bigint                                  NOT NULL,
    `amount`             decimal(38, 2)                          NOT NULL COMMENT '金额',
    `balance_after`      decimal(38, 2)                          DEFAULT NULL COMMENT '交易后余额',
    `balance_before`     decimal(38, 2)                          DEFAULT NULL COMMENT '交易前余额',
    `created_at`         datetime(6) DEFAULT NULL COMMENT '创建时间',
    `created_by`         bigint                                  DEFAULT NULL COMMENT '创建人',
    `is_initial`         bit(1)                                  DEFAULT NULL COMMENT '是否期初',
    `merchant_id`        bigint                                  NOT NULL,
    `order_id`           bigint                                  DEFAULT NULL COMMENT '单据Id',
    `remarks`            varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
    `supplier_flow_type` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '操作类型',
    `supplier_id`        bigint                                  NOT NULL COMMENT '货商Id',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_supplier_flow
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_system_config
-- ----------------------------
DROP TABLE IF EXISTS `jxc_system_config`;
CREATE TABLE `jxc_system_config`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `config_type` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '参数类型',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '状态',
  `merchant_id` bigint NOT NULL,
  `name` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_system_config
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_unit
-- ----------------------------
DROP TABLE IF EXISTS `jxc_unit`;
CREATE TABLE `jxc_unit`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `merchant_id` bigint NOT NULL,
  `name` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK1cmb0yvrhifw0otep8ogi1l49` (`merchant_id`,`account_book_id`,`name`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_unit
-- ----------------------------
INSERT INTO `jxc_unit`
VALUES ('2', '1', '1', '个');

-- ----------------------------
-- Table structure for jxc_verification
-- ----------------------------
DROP TABLE IF EXISTS `jxc_verification`;
CREATE TABLE `jxc_verification`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `merchant_id`     bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_verification
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_verification_item
-- ----------------------------
DROP TABLE IF EXISTS `jxc_verification_item`;
CREATE TABLE `jxc_verification_item`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `merchant_id`     bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_verification_item
-- ----------------------------

-- ----------------------------
-- Table structure for jxc_warehouse
-- ----------------------------
DROP TABLE IF EXISTS `jxc_warehouse`;
CREATE TABLE `jxc_warehouse`
(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `enabled` bit(1)                                 NOT NULL DEFAULT b'1' COMMENT '状态',
  `merchant_id` bigint NOT NULL,
  `name`    varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `system_default` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否默认',
  `address` varchar(255) COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '地址',
  `code`    varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '编码',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKthbgvaxdw8s6ko243r2djfc0e` (`merchant_id`,`account_book_id`,`name`),
  UNIQUE KEY `UKtepf0b4kaobx6cnucq5yaetyh` (`merchant_id`,`account_book_id`,`code`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of jxc_warehouse
-- ----------------------------
INSERT INTO `jxc_warehouse`
VALUES ('5', '1', '', '1', '213213', '', '123213', '2122');
