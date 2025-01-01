/*
Navicat MySQL Data Transfer

Source Server         : local
Source Server Version : 80028
Source Host           : localhost:3306
Source Database       : jxc

Target Server Type    : MYSQL
Target Server Version : 80028
File Encoding         : 65001

Date: 2025-01-01 23:22:53
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for s_account
-- ----------------------------
DROP TABLE IF EXISTS `s_account`;
CREATE TABLE `s_account` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `account_type` varchar(32) DEFAULT '银行存款' COMMENT '账户类别',
  `balance` decimal(38,2) DEFAULT NULL COMMENT '账户余额',
  `code` varchar(32) NOT NULL COMMENT '编码',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '状态',
  `merchant_id` bigint NOT NULL,
  `name` varchar(32) NOT NULL COMMENT '名称',
  `system_default` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否系统默认',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKstdtk9c0iaa5uxufahxanaora` (`merchant_id`,`account_book_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_account
-- ----------------------------

-- ----------------------------
-- Table structure for s_account_book
-- ----------------------------
DROP TABLE IF EXISTS `s_account_book`;
CREATE TABLE `s_account_book` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `checkout_date` date DEFAULT NULL COMMENT '结账日期',
  `current` bit(1) DEFAULT NULL COMMENT '当前账套',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '状态',
  `merchant_id` bigint NOT NULL,
  `name` varchar(64) NOT NULL COMMENT '名称',
  `start_date` date DEFAULT NULL COMMENT '启用日期',
  `warehouse_id` bigint DEFAULT NULL COMMENT '默认仓库',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKf3hqsnnsuubp2dp8m2xi3xr2` (`name`,`merchant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_account_book
-- ----------------------------
INSERT INTO `s_account_book` VALUES ('1', null, '', '', '1', '纷析云', '2024-12-31', null);

-- ----------------------------
-- Table structure for s_account_flow
-- ----------------------------
DROP TABLE IF EXISTS `s_account_flow`;
CREATE TABLE `s_account_flow` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_flow_type` varchar(255) NOT NULL COMMENT '操作类型',
  `account_id` bigint NOT NULL COMMENT '账户Id',
  `amount` decimal(38,2) NOT NULL COMMENT '金额',
  `audit_id` bigint DEFAULT NULL COMMENT '审核人',
  `audit_time` datetime(6) DEFAULT NULL COMMENT '审核时间',
  `balance_after` decimal(38,2) DEFAULT NULL COMMENT '交易后余额',
  `balance_before` decimal(38,2) DEFAULT NULL COMMENT '交易前余额',
  `flow_time` datetime(6) DEFAULT NULL COMMENT '交易时间',
  `remarks` varchar(255) DEFAULT NULL COMMENT '备注',
  `user_id` bigint DEFAULT NULL COMMENT '操作人Id',
  `voucher_id` bigint DEFAULT NULL COMMENT '单据Id',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_account_flow
-- ----------------------------

-- ----------------------------
-- Table structure for s_account_transfer
-- ----------------------------
DROP TABLE IF EXISTS `s_account_transfer`;
CREATE TABLE `s_account_transfer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_account_transfer
-- ----------------------------

-- ----------------------------
-- Table structure for s_account_transfer_item
-- ----------------------------
DROP TABLE IF EXISTS `s_account_transfer_item`;
CREATE TABLE `s_account_transfer_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_account_transfer_item
-- ----------------------------

-- ----------------------------
-- Table structure for s_account_type
-- ----------------------------
DROP TABLE IF EXISTS `s_account_type`;
CREATE TABLE `s_account_type` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `cost_type` varchar(32) DEFAULT '收入' COMMENT '收支类别',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '状态',
  `merchant_id` bigint NOT NULL,
  `name` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKh07vonilujq485owg0jdscibs` (`merchant_id`,`account_book_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_account_type
-- ----------------------------

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

-- ----------------------------
-- Table structure for s_checkout
-- ----------------------------
DROP TABLE IF EXISTS `s_checkout`;
CREATE TABLE `s_checkout` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `check_date` date DEFAULT NULL COMMENT '结账日期',
  `check_id` bigint DEFAULT NULL COMMENT '结账人',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `merchant_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKqpanwltshydnp8du0steoo29p` (`check_date`,`account_book_id`,`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_checkout
-- ----------------------------

-- ----------------------------
-- Table structure for s_code_rule
-- ----------------------------
DROP TABLE IF EXISTS `s_code_rule`;
CREATE TABLE `s_code_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `date_format` varchar(255) DEFAULT NULL COMMENT '日期格式',
  `document_type` varchar(255) NOT NULL COMMENT '单据类型',
  `merchant_id` bigint NOT NULL,
  `name` varchar(32) NOT NULL COMMENT '规则名称',
  `prefix` varchar(255) DEFAULT NULL COMMENT '前缀',
  `reset_cycle` varchar(255) NOT NULL COMMENT '重置周期',
  `serial_number_length` int DEFAULT NULL COMMENT '流水号位数',
  `start_value` int DEFAULT NULL COMMENT '起始值',
  `system_default` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否系统默认',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_code_rule
-- ----------------------------

-- ----------------------------
-- Table structure for s_cost_adjustment
-- ----------------------------
DROP TABLE IF EXISTS `s_cost_adjustment`;
CREATE TABLE `s_cost_adjustment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `adjustment_amount` decimal(38,2) DEFAULT NULL COMMENT '调整金额',
  `adjustment_type` varchar(20) NOT NULL DEFAULT '采购成本调整' COMMENT '调整类型',
  `approved_at` datetime(6) DEFAULT NULL COMMENT '审核时间',
  `approved_by` bigint DEFAULT NULL COMMENT '审核人',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `djustment_date` datetime(6) DEFAULT NULL COMMENT '调整日期',
  `merchant_id` bigint NOT NULL,
  `order_no` varchar(255) DEFAULT NULL COMMENT '单据编号',
  `order_status` varchar(20) NOT NULL DEFAULT '已保存' COMMENT '订单状态',
  `remarks` varchar(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_cost_adjustment
-- ----------------------------

-- ----------------------------
-- Table structure for s_cost_adjustment_item
-- ----------------------------
DROP TABLE IF EXISTS `s_cost_adjustment_item`;
CREATE TABLE `s_cost_adjustment_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `adjustment_amount` decimal(38,2) DEFAULT NULL COMMENT '调整金额：本次调整的总金额（调整数量 * (调整后成本 - 调整前成本)）',
  `base_unit_id` bigint DEFAULT NULL COMMENT '基本单位ID',
  `cost_adjustment_id` bigint DEFAULT NULL COMMENT '成本调整单主表',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `merchant_id` bigint NOT NULL,
  `new_cost` decimal(38,2) DEFAULT NULL COMMENT '调整后的产品单位成本',
  `original_cost` decimal(38,2) DEFAULT NULL COMMENT '调整前的产品单位成本',
  `product_id` bigint DEFAULT NULL COMMENT '产品ID',
  `remarks` varchar(255) DEFAULT NULL COMMENT '备注',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_cost_adjustment_item
-- ----------------------------

-- ----------------------------
-- Table structure for s_customer
-- ----------------------------
DROP TABLE IF EXISTS `s_customer`;
CREATE TABLE `s_customer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `code` varchar(255) DEFAULT NULL COMMENT '编码',
  `contact` varchar(255) DEFAULT NULL COMMENT '联系人',
  `customer_category_id` bigint NOT NULL COMMENT '客户分类ID',
  `customer_level_id` bigint NOT NULL COMMENT '客户等级ID',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '状态',
  `merchant_id` bigint NOT NULL,
  `name` varchar(32) NOT NULL COMMENT '名称',
  `phone` varchar(255) DEFAULT NULL COMMENT '电话',
  `remarks` varchar(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKm4hkkend5xhf5s8xdgmfyre32` (`merchant_id`,`account_book_id`,`code`)
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
  `name` varchar(32) NOT NULL COMMENT '名称',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKhh5mp4a4c6r73lq4r2fs228n3` (`merchant_id`,`account_book_id`,`name`)
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
  UNIQUE KEY `UK69dbar392i8122s24nxu1uo9f` (`merchant_id`,`account_book_id`,`name`)
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
  `auxiliary_unit_prices` json DEFAULT NULL COMMENT '客户单位辅助价格',
  `customer_level_id` bigint DEFAULT NULL COMMENT '客户等级ID',
  `merchant_id` bigint NOT NULL,
  `price` decimal(38,2) DEFAULT NULL COMMENT '基础单位价格',
  `product_id` bigint DEFAULT NULL COMMENT '产品ID',
  `unit_id` bigint DEFAULT NULL COMMENT '基础单位',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK4acgpx67ktcp094v7jb8irx0` (`merchant_id`,`account_book_id`,`product_id`,`customer_level_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_customer_level_price
-- ----------------------------

-- ----------------------------
-- Table structure for s_inventory
-- ----------------------------
DROP TABLE IF EXISTS `s_inventory`;
CREATE TABLE `s_inventory` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `average_cost` decimal(38,2) DEFAULT NULL COMMENT '平均成本',
  `base_unit_id` bigint DEFAULT NULL COMMENT '基础单位ID',
  `current_quantity` int DEFAULT NULL COMMENT '库存数量',
  `product_id` bigint DEFAULT NULL COMMENT '产品ID',
  `safety_stock` int DEFAULT NULL COMMENT '安全库存',
  `total_cost` decimal(38,2) DEFAULT NULL COMMENT '成本总计',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_inventory
-- ----------------------------

-- ----------------------------
-- Table structure for s_inventory_item
-- ----------------------------
DROP TABLE IF EXISTS `s_inventory_item`;
CREATE TABLE `s_inventory_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `base_unit_id` bigint DEFAULT NULL COMMENT '基础单位ID',
  `batch_number` varchar(255) DEFAULT NULL COMMENT '批次号',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `merchant_id` bigint NOT NULL,
  `operation_type` smallint DEFAULT NULL COMMENT '操作类型：入库、出库、调拨',
  `product_id` bigint DEFAULT NULL COMMENT '产品ID',
  `quantity` int DEFAULT NULL COMMENT '正数为入库，负数为出库',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `order_id` bigint DEFAULT NULL COMMENT '关联单据ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_inventory_item
-- ----------------------------

-- ----------------------------
-- Table structure for s_inventory_transfer
-- ----------------------------
DROP TABLE IF EXISTS `s_inventory_transfer`;
CREATE TABLE `s_inventory_transfer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `from_warehouse_id` bigint DEFAULT NULL COMMENT '调出仓库',
  `to_warehouse_id` bigint DEFAULT NULL COMMENT '调入仓库',
  `account_book_id` bigint NOT NULL,
  `approved_at` datetime(6) DEFAULT NULL COMMENT '审核时间',
  `approved_by` bigint DEFAULT NULL COMMENT '审核人',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `merchant_id` bigint NOT NULL,
  `order_status` varchar(20) NOT NULL DEFAULT '已保存' COMMENT '订单状态',
  `remarks` varchar(255) DEFAULT NULL COMMENT '备注',
  `transfer_date` datetime(6) DEFAULT NULL COMMENT '调拨日期',
  `order_no` varchar(255) DEFAULT NULL COMMENT '单据编号',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_inventory_transfer
-- ----------------------------

-- ----------------------------
-- Table structure for s_inventory_transfer_item
-- ----------------------------
DROP TABLE IF EXISTS `s_inventory_transfer_item`;
CREATE TABLE `s_inventory_transfer_item` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_inventory_transfer_item
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
) ENGINE=InnoDB AUTO_INCREMENT=79 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
INSERT INTO `s_menu` VALUES ('22', 'AccountList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '账户管理', '10', '2', '');
INSERT INTO `s_menu` VALUES ('23', 'AccountTypeList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '收支类型', '10', '3', '');
INSERT INTO `s_menu` VALUES ('24', 'SettlementMethodList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '结算方式', '10', '4', '');
INSERT INTO `s_menu` VALUES ('25', null, '', 'h-icon-plus', 'MERCHANT', 'MERCHANT', 'MENU', '采购管理', null, '1', '');
INSERT INTO `s_menu` VALUES ('26', null, '', 'h-icon-complete', 'MERCHANT', 'MERCHANT', 'MENU', '销售管理', null, '2', '');
INSERT INTO `s_menu` VALUES ('27', null, '', 'h-icon-check', 'MERCHANT', 'MERCHANT', 'MENU', '库存管理', null, '3', '');
INSERT INTO `s_menu` VALUES ('28', null, '', 'h-icon-bell', 'MERCHANT', 'MERCHANT', 'MENU', '资金账户', null, '4', '');
INSERT INTO `s_menu` VALUES ('29', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '资金单据', '28', '0', '');
INSERT INTO `s_menu` VALUES ('30', 'ReceiptVoucher ', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '收款单', '29', '0', '');
INSERT INTO `s_menu` VALUES ('31', 'PaymentVoucher ', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '付款单', '29', '1', '');
INSERT INTO `s_menu` VALUES ('32', 'WriteOff', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '核销单', '29', '2', '');
INSERT INTO `s_menu` VALUES ('33', 'AccountTransfer', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '资金转账', '29', '3', '');
INSERT INTO `s_menu` VALUES ('34', 'OtherIncome', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '其他收款单', '29', '4', '');
INSERT INTO `s_menu` VALUES ('35', 'OtherExpense', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '其他付款单', '29', '5', '');
INSERT INTO `s_menu` VALUES ('36', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '资金报表', '28', '1', '');
INSERT INTO `s_menu` VALUES ('37', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '往来单位欠款表', '36', '0', '');
INSERT INTO `s_menu` VALUES ('38', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '客户对账单', '36', '1', '');
INSERT INTO `s_menu` VALUES ('39', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '供货商对账单', '36', '2', '');
INSERT INTO `s_menu` VALUES ('40', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '应收账款明细表', '36', '3', '');
INSERT INTO `s_menu` VALUES ('41', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '应付账款明细表', '36', '4', '');
INSERT INTO `s_menu` VALUES ('42', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '其他收支明细表', '36', '5', '');
INSERT INTO `s_menu` VALUES ('43', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '价格设置', '1', '3', '');
INSERT INTO `s_menu` VALUES ('44', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '产品价格资料', '43', '0', '');
INSERT INTO `s_menu` VALUES ('45', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '产品价格记录', '43', '1', '');
INSERT INTO `s_menu` VALUES ('46', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '编码规则设置', '15', '4', '');
INSERT INTO `s_menu` VALUES ('47', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '结账/反结账', '15', '5', '');
INSERT INTO `s_menu` VALUES ('48', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '打印模板', '15', '6', '');
INSERT INTO `s_menu` VALUES ('49', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '备份与恢复', '15', '7', '');
INSERT INTO `s_menu` VALUES ('50', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '操作日志', '15', '8', '');
INSERT INTO `s_menu` VALUES ('51', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '采购单据', '25', '0', '');
INSERT INTO `s_menu` VALUES ('52', 'PurchaseOrder', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '采购订单', '51', '0', '');
INSERT INTO `s_menu` VALUES ('53', 'PurchaseInbound', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '采购入库单', '51', '1', '');
INSERT INTO `s_menu` VALUES ('54', 'PurchaseReturn', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '采购退货单', '51', '2', '');
INSERT INTO `s_menu` VALUES ('55', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '采购报表', '25', '1', '');
INSERT INTO `s_menu` VALUES ('56', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '采购明细表', '55', '0', '');
INSERT INTO `s_menu` VALUES ('57', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '采购汇总表', '55', '1', '');
INSERT INTO `s_menu` VALUES ('58', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '销售单据', '26', '0', '');
INSERT INTO `s_menu` VALUES ('59', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '销售报表', '26', '1', '');
INSERT INTO `s_menu` VALUES ('60', 'SalesOrder ', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '销售订单', '58', '0', '');
INSERT INTO `s_menu` VALUES ('61', 'SalesOutbound', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '销售出库单', '58', '1', '');
INSERT INTO `s_menu` VALUES ('62', 'SalesReturn ', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '销售退货单', '58', '2', '');
INSERT INTO `s_menu` VALUES ('63', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '销售明细表', '59', '0', '');
INSERT INTO `s_menu` VALUES ('64', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '销售汇总表', '59', '1', '');
INSERT INTO `s_menu` VALUES ('65', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '销售利润表', '59', '2', '');
INSERT INTO `s_menu` VALUES ('66', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '销售排行表', '59', '3', '');
INSERT INTO `s_menu` VALUES ('67', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '库存单据', '27', '0', '');
INSERT INTO `s_menu` VALUES ('68', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '库存报表', '27', '1', '');
INSERT INTO `s_menu` VALUES ('69', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '库存核算', '27', '2', '');
INSERT INTO `s_menu` VALUES ('70', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '成本调整单', '69', '0', '');
INSERT INTO `s_menu` VALUES ('71', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '重算成本', '69', '1', '');
INSERT INTO `s_menu` VALUES ('72', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '库存余额表', '68', '0', '');
INSERT INTO `s_menu` VALUES ('73', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '进销存明细表', '68', '1', '');
INSERT INTO `s_menu` VALUES ('74', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '进销存汇总表', '68', '2', '');
INSERT INTO `s_menu` VALUES ('75', 'InventoryTransfer', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '调拨单', '67', '0', '');
INSERT INTO `s_menu` VALUES ('76', 'StockTake ', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '盘点单', '67', '1', '');
INSERT INTO `s_menu` VALUES ('77', 'OtherOutbound', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '其他出库单', '67', '2', '');
INSERT INTO `s_menu` VALUES ('78', 'OtherInbound', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '其他入库单', '67', '3', '');

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
  `email` varchar(255) DEFAULT NULL COMMENT '邮箱',
  `enabled` bit(1) DEFAULT NULL COMMENT '是否启用',
  `mobile` varchar(255) DEFAULT NULL COMMENT '电话号码',
  `name` varchar(255) DEFAULT NULL COMMENT '商户名称',
  `service_end_date` date DEFAULT NULL COMMENT '服务结束时间',
  `service_start_date` date DEFAULT NULL COMMENT '服务开始时间',
  `costing_method` smallint DEFAULT NULL COMMENT '存货计价方式',
  `stock_field` bit(1) DEFAULT NULL COMMENT '允许负库存',
  `contact` varchar(255) DEFAULT NULL COMMENT '联系人',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建日期',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_merchant
-- ----------------------------
INSERT INTO `s_merchant` VALUES ('1', '杭州', '32f90df1-c739-463b-8218-715437765807', null, '', '13944878765', '纷析云', '2024-12-01', '2024-12-01', null, null, null, null);

-- ----------------------------
-- Table structure for s_merchant_menu
-- ----------------------------
DROP TABLE IF EXISTS `s_merchant_menu`;
CREATE TABLE `s_merchant_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `menu_id` bigint NOT NULL,
  `merchant_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=228 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_merchant_menu
-- ----------------------------
INSERT INTO `s_merchant_menu` VALUES ('156', '1', '1');
INSERT INTO `s_merchant_menu` VALUES ('157', '6', '1');
INSERT INTO `s_merchant_menu` VALUES ('158', '7', '1');
INSERT INTO `s_merchant_menu` VALUES ('159', '8', '1');
INSERT INTO `s_merchant_menu` VALUES ('160', '9', '1');
INSERT INTO `s_merchant_menu` VALUES ('161', '10', '1');
INSERT INTO `s_merchant_menu` VALUES ('162', '12', '1');
INSERT INTO `s_merchant_menu` VALUES ('163', '14', '1');
INSERT INTO `s_merchant_menu` VALUES ('164', '15', '1');
INSERT INTO `s_merchant_menu` VALUES ('165', '16', '1');
INSERT INTO `s_merchant_menu` VALUES ('166', '17', '1');
INSERT INTO `s_merchant_menu` VALUES ('167', '18', '1');
INSERT INTO `s_merchant_menu` VALUES ('168', '19', '1');
INSERT INTO `s_merchant_menu` VALUES ('169', '20', '1');
INSERT INTO `s_merchant_menu` VALUES ('170', '21', '1');
INSERT INTO `s_merchant_menu` VALUES ('171', '22', '1');
INSERT INTO `s_merchant_menu` VALUES ('172', '23', '1');
INSERT INTO `s_merchant_menu` VALUES ('173', '24', '1');
INSERT INTO `s_merchant_menu` VALUES ('174', '25', '1');
INSERT INTO `s_merchant_menu` VALUES ('175', '26', '1');
INSERT INTO `s_merchant_menu` VALUES ('176', '27', '1');
INSERT INTO `s_merchant_menu` VALUES ('177', '28', '1');
INSERT INTO `s_merchant_menu` VALUES ('178', '29', '1');
INSERT INTO `s_merchant_menu` VALUES ('179', '30', '1');
INSERT INTO `s_merchant_menu` VALUES ('180', '31', '1');
INSERT INTO `s_merchant_menu` VALUES ('181', '32', '1');
INSERT INTO `s_merchant_menu` VALUES ('182', '33', '1');
INSERT INTO `s_merchant_menu` VALUES ('183', '34', '1');
INSERT INTO `s_merchant_menu` VALUES ('184', '35', '1');
INSERT INTO `s_merchant_menu` VALUES ('185', '36', '1');
INSERT INTO `s_merchant_menu` VALUES ('186', '37', '1');
INSERT INTO `s_merchant_menu` VALUES ('187', '38', '1');
INSERT INTO `s_merchant_menu` VALUES ('188', '39', '1');
INSERT INTO `s_merchant_menu` VALUES ('189', '40', '1');
INSERT INTO `s_merchant_menu` VALUES ('190', '41', '1');
INSERT INTO `s_merchant_menu` VALUES ('191', '42', '1');
INSERT INTO `s_merchant_menu` VALUES ('192', '43', '1');
INSERT INTO `s_merchant_menu` VALUES ('193', '44', '1');
INSERT INTO `s_merchant_menu` VALUES ('194', '45', '1');
INSERT INTO `s_merchant_menu` VALUES ('195', '46', '1');
INSERT INTO `s_merchant_menu` VALUES ('196', '47', '1');
INSERT INTO `s_merchant_menu` VALUES ('197', '48', '1');
INSERT INTO `s_merchant_menu` VALUES ('198', '49', '1');
INSERT INTO `s_merchant_menu` VALUES ('199', '50', '1');
INSERT INTO `s_merchant_menu` VALUES ('200', '51', '1');
INSERT INTO `s_merchant_menu` VALUES ('201', '52', '1');
INSERT INTO `s_merchant_menu` VALUES ('202', '53', '1');
INSERT INTO `s_merchant_menu` VALUES ('203', '54', '1');
INSERT INTO `s_merchant_menu` VALUES ('204', '55', '1');
INSERT INTO `s_merchant_menu` VALUES ('205', '56', '1');
INSERT INTO `s_merchant_menu` VALUES ('206', '57', '1');
INSERT INTO `s_merchant_menu` VALUES ('207', '58', '1');
INSERT INTO `s_merchant_menu` VALUES ('208', '59', '1');
INSERT INTO `s_merchant_menu` VALUES ('209', '60', '1');
INSERT INTO `s_merchant_menu` VALUES ('210', '61', '1');
INSERT INTO `s_merchant_menu` VALUES ('211', '62', '1');
INSERT INTO `s_merchant_menu` VALUES ('212', '63', '1');
INSERT INTO `s_merchant_menu` VALUES ('213', '64', '1');
INSERT INTO `s_merchant_menu` VALUES ('214', '65', '1');
INSERT INTO `s_merchant_menu` VALUES ('215', '66', '1');
INSERT INTO `s_merchant_menu` VALUES ('216', '67', '1');
INSERT INTO `s_merchant_menu` VALUES ('217', '68', '1');
INSERT INTO `s_merchant_menu` VALUES ('218', '69', '1');
INSERT INTO `s_merchant_menu` VALUES ('219', '70', '1');
INSERT INTO `s_merchant_menu` VALUES ('220', '71', '1');
INSERT INTO `s_merchant_menu` VALUES ('221', '72', '1');
INSERT INTO `s_merchant_menu` VALUES ('222', '73', '1');
INSERT INTO `s_merchant_menu` VALUES ('223', '74', '1');
INSERT INTO `s_merchant_menu` VALUES ('224', '75', '1');
INSERT INTO `s_merchant_menu` VALUES ('225', '76', '1');
INSERT INTO `s_merchant_menu` VALUES ('226', '77', '1');
INSERT INTO `s_merchant_menu` VALUES ('227', '78', '1');

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
INSERT INTO `s_merchant_user` VALUES ('1', '', '2025-01-01 23:19:08.924364', '李泽龙', '$2a$10$MPZ4oROTiWvlKqiq79Sk7.TMcY3D8p8edgUpO5UarsCMroUeChSQC', '', '13944878765');

-- ----------------------------
-- Table structure for s_other_inbound
-- ----------------------------
DROP TABLE IF EXISTS `s_other_inbound`;
CREATE TABLE `s_other_inbound` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `approved_at` datetime(6) DEFAULT NULL COMMENT '审核时间',
  `approved_by` bigint DEFAULT NULL COMMENT '审核人',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `customer_id` bigint DEFAULT NULL COMMENT '客户ID',
  `discount_amount` decimal(38,2) DEFAULT NULL COMMENT '折扣金额',
  `final_amount` decimal(38,2) DEFAULT NULL COMMENT '折后金额',
  `inbound_date` datetime(6) DEFAULT NULL COMMENT '入库日期',
  `inbound_type` varchar(20) NOT NULL DEFAULT '其他入库' COMMENT '入库类型',
  `merchant_id` bigint NOT NULL,
  `order_status` varchar(20) NOT NULL DEFAULT '已保存' COMMENT '订单状态',
  `remarks` varchar(255) DEFAULT NULL COMMENT '备注',
  `supplier_id` bigint DEFAULT NULL COMMENT '供货商ID',
  `total_amount` decimal(38,2) DEFAULT NULL COMMENT '订单金额',
  `order_no` varchar(255) DEFAULT NULL COMMENT '单据编号',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_other_inbound
-- ----------------------------

-- ----------------------------
-- Table structure for s_other_inbound_item
-- ----------------------------
DROP TABLE IF EXISTS `s_other_inbound_item`;
CREATE TABLE `s_other_inbound_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `base_unit_id` bigint DEFAULT NULL COMMENT '基本单位ID',
  `batch_number` varchar(255) DEFAULT NULL COMMENT '批次号',
  `conversion_rate` decimal(38,2) DEFAULT NULL COMMENT '换算率 (基本单位到辅助单位的换算率，例如：1箱=12个，则换算率为12。如果未使用辅助单位，则为1)',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `discount_rate` decimal(38,2) DEFAULT NULL COMMENT '折扣率',
  `discount_value` decimal(38,2) DEFAULT NULL COMMENT '折扣金额',
  `merchant_id` bigint NOT NULL,
  `other_inbound_id` bigint DEFAULT NULL COMMENT '其他入库主表ID',
  `product_id` bigint DEFAULT NULL COMMENT '产品ID',
  `quantity` double DEFAULT NULL COMMENT '数量（以基本单位计）',
  `secondary_quantity` double DEFAULT NULL COMMENT '辅助单位数量 (可为空',
  `secondary_unit_id` bigint DEFAULT NULL COMMENT '辅助单位ID(可为空)',
  `subtotal` decimal(38,2) DEFAULT NULL COMMENT '小计 (quantity * unitPrice * (1 - discount_value/100) 或 quantity * unitPrice - discount_value，根据折扣类型计算)',
  `unit_price` decimal(38,2) DEFAULT NULL COMMENT '单价（以基本单位计）',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_other_inbound_item
-- ----------------------------

-- ----------------------------
-- Table structure for s_other_outbound
-- ----------------------------
DROP TABLE IF EXISTS `s_other_outbound`;
CREATE TABLE `s_other_outbound` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `approved_at` datetime(6) DEFAULT NULL COMMENT '审核时间',
  `approved_by` bigint DEFAULT NULL COMMENT '审核人',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `customer_id` bigint DEFAULT NULL COMMENT '客户ID',
  `discount_amount` decimal(38,2) DEFAULT NULL COMMENT '折扣金额',
  `final_amount` decimal(38,2) DEFAULT NULL COMMENT '折后金额',
  `inbound_date` datetime(6) DEFAULT NULL COMMENT '入库日期',
  `merchant_id` bigint NOT NULL,
  `order_status` varchar(20) NOT NULL DEFAULT '已保存' COMMENT '订单状态',
  `outbound_type` varchar(20) NOT NULL DEFAULT '其他出库' COMMENT '出库类型',
  `remarks` varchar(255) DEFAULT NULL COMMENT '备注',
  `supplier_id` bigint DEFAULT NULL COMMENT '供货商ID',
  `total_amount` decimal(38,2) DEFAULT NULL COMMENT '订单金额',
  `order_no` varchar(255) DEFAULT NULL COMMENT '单据编号',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_other_outbound
-- ----------------------------

-- ----------------------------
-- Table structure for s_other_outbound_item
-- ----------------------------
DROP TABLE IF EXISTS `s_other_outbound_item`;
CREATE TABLE `s_other_outbound_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `base_unit_id` bigint DEFAULT NULL COMMENT '基本单位ID',
  `batch_number` varchar(255) DEFAULT NULL COMMENT '批次号',
  `conversion_rate` decimal(38,2) DEFAULT NULL COMMENT '换算率 (基本单位到辅助单位的换算率，例如：1箱=12个，则换算率为12。如果未使用辅助单位，则为1)',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `discount_rate` decimal(38,2) DEFAULT NULL COMMENT '折扣率',
  `discount_value` decimal(38,2) DEFAULT NULL COMMENT '折扣金额',
  `merchant_id` bigint NOT NULL,
  `other_outbound_id` bigint DEFAULT NULL COMMENT '其他出库主表ID',
  `product_id` bigint DEFAULT NULL COMMENT '产品ID',
  `quantity` double DEFAULT NULL COMMENT '数量（以基本单位计）',
  `secondary_quantity` double DEFAULT NULL COMMENT '辅助单位数量 (可为空',
  `secondary_unit_id` bigint DEFAULT NULL COMMENT '辅助单位ID(可为空)',
  `subtotal` decimal(38,2) DEFAULT NULL COMMENT '小计 (quantity * unitPrice * (1 - discount_value/100) 或 quantity * unitPrice - discount_value，根据折扣类型计算)',
  `unit_price` decimal(38,2) DEFAULT NULL COMMENT '单价（以基本单位计）',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_other_outbound_item
-- ----------------------------

-- ----------------------------
-- Table structure for s_other_payment
-- ----------------------------
DROP TABLE IF EXISTS `s_other_payment`;
CREATE TABLE `s_other_payment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_other_payment
-- ----------------------------

-- ----------------------------
-- Table structure for s_other_payment_item
-- ----------------------------
DROP TABLE IF EXISTS `s_other_payment_item`;
CREATE TABLE `s_other_payment_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_other_payment_item
-- ----------------------------

-- ----------------------------
-- Table structure for s_other_receipt
-- ----------------------------
DROP TABLE IF EXISTS `s_other_receipt`;
CREATE TABLE `s_other_receipt` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_other_receipt
-- ----------------------------

-- ----------------------------
-- Table structure for s_other_receipt_item
-- ----------------------------
DROP TABLE IF EXISTS `s_other_receipt_item`;
CREATE TABLE `s_other_receipt_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_other_receipt_item
-- ----------------------------

-- ----------------------------
-- Table structure for s_payment
-- ----------------------------
DROP TABLE IF EXISTS `s_payment`;
CREATE TABLE `s_payment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_payment
-- ----------------------------

-- ----------------------------
-- Table structure for s_payment_item
-- ----------------------------
DROP TABLE IF EXISTS `s_payment_item`;
CREATE TABLE `s_payment_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_payment_item
-- ----------------------------

-- ----------------------------
-- Table structure for s_payment_method
-- ----------------------------
DROP TABLE IF EXISTS `s_payment_method`;
CREATE TABLE `s_payment_method` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '状态',
  `merchant_id` bigint NOT NULL,
  `name` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKmlqf8e5payxbvg5mxv4asxjrj` (`merchant_id`,`account_book_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_payment_method
-- ----------------------------

-- ----------------------------
-- Table structure for s_pricing_policy
-- ----------------------------
DROP TABLE IF EXISTS `s_pricing_policy`;
CREATE TABLE `s_pricing_policy` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '状态',
  `merchant_id` bigint NOT NULL,
  `name` varchar(32) NOT NULL,
  `price_source` varchar(255) NOT NULL COMMENT '价格来源',
  `price_type` varchar(255) NOT NULL COMMENT '价格类型',
  `priority` int DEFAULT NULL COMMENT '优先级',
  `remarks` varchar(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_pricing_policy
-- ----------------------------

-- ----------------------------
-- Table structure for s_pricing_strategy
-- ----------------------------
DROP TABLE IF EXISTS `s_pricing_strategy`;
CREATE TABLE `s_pricing_strategy` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '状态',
  `merchant_id` bigint NOT NULL,
  `name` varchar(32) NOT NULL,
  `price_source` varchar(255) NOT NULL COMMENT '价格来源',
  `price_type` varchar(255) NOT NULL COMMENT '价格类型',
  `priority` int DEFAULT NULL COMMENT '优先级',
  `remarks` varchar(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_pricing_strategy
-- ----------------------------

-- ----------------------------
-- Table structure for s_product
-- ----------------------------
DROP TABLE IF EXISTS `s_product`;
CREATE TABLE `s_product` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `alert_quantity` int DEFAULT NULL COMMENT '预警库存',
  `auxiliary_unit_prices` json DEFAULT NULL COMMENT '辅助单位价格',
  `barcode` varchar(32) DEFAULT NULL COMMENT '条码',
  `code` varchar(64) NOT NULL COMMENT '编码',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `enable_auxiliary_unit` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否启用辅助单位',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '状态',
  `img_path` varchar(255) DEFAULT NULL COMMENT '商品图片',
  `merchant_id` bigint NOT NULL,
  `name` varchar(64) NOT NULL COMMENT '名称',
  `pinyin` varchar(32) DEFAULT NULL COMMENT '拼音',
  `product_category_id` bigint NOT NULL COMMENT '产品分类',
  `purchase_price` decimal(38,2) NOT NULL COMMENT '预计进货价（基础单位）',
  `remarks` varchar(255) DEFAULT NULL COMMENT '备注',
  `sort` int DEFAULT NULL COMMENT '排序',
  `specification` varchar(255) DEFAULT NULL COMMENT '规格',
  `stock_quantity` int DEFAULT NULL COMMENT '库存数量',
  `unit_id` bigint NOT NULL COMMENT '基础单位',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKned9ta6s8ormce3qpx4qdplou` (`merchant_id`,`account_book_id`,`code`)
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
  UNIQUE KEY `UKah8t0d655vjgr864ata6tkw6u` (`merchant_id`,`account_book_id`,`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_product_category
-- ----------------------------

-- ----------------------------
-- Table structure for s_purchase_inbound
-- ----------------------------
DROP TABLE IF EXISTS `s_purchase_inbound`;
CREATE TABLE `s_purchase_inbound` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `approved_at` datetime(6) DEFAULT NULL COMMENT '审核时间',
  `approved_by` bigint DEFAULT NULL COMMENT '审核人',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `discount_amount` decimal(38,2) DEFAULT NULL COMMENT '折扣金额',
  `final_amount` decimal(38,2) DEFAULT NULL COMMENT '折后金额',
  `inbound_date` datetime(6) DEFAULT NULL COMMENT '入库日期',
  `merchant_id` bigint NOT NULL,
  `order_id` bigint DEFAULT NULL COMMENT '采购订单ID',
  `order_status` varchar(20) NOT NULL DEFAULT '已保存' COMMENT '订单状态',
  `remarks` varchar(255) DEFAULT NULL COMMENT '备注',
  `supplier_id` bigint DEFAULT NULL COMMENT '供货商ID',
  `total_amount` decimal(38,2) DEFAULT NULL COMMENT '订单金额',
  `order_no` varchar(255) DEFAULT NULL COMMENT '单据编号',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_purchase_inbound
-- ----------------------------

-- ----------------------------
-- Table structure for s_purchase_inbound_item
-- ----------------------------
DROP TABLE IF EXISTS `s_purchase_inbound_item`;
CREATE TABLE `s_purchase_inbound_item` (
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
  `purchase_inbound_id` bigint DEFAULT NULL COMMENT '采购入库主表ID',
  `quantity` double DEFAULT NULL COMMENT '数量（以基本单位计）',
  `secondary_quantity` double DEFAULT NULL COMMENT '辅助单位数量 (可为空',
  `secondary_unit_id` bigint DEFAULT NULL COMMENT '辅助单位ID(可为空)',
  `subtotal` decimal(38,2) DEFAULT NULL COMMENT '小计 (quantity * unit_price * (1 - discount_value/100) 或 quantity * unit_price - discount_value，根据折扣类型计算)',
  `unit_price` decimal(38,2) DEFAULT NULL COMMENT '单价（以基本单位计）',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `batch_number` varchar(255) DEFAULT NULL COMMENT '批次号',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_purchase_inbound_item
-- ----------------------------

-- ----------------------------
-- Table structure for s_purchase_order
-- ----------------------------
DROP TABLE IF EXISTS `s_purchase_order`;
CREATE TABLE `s_purchase_order` (
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
  `order_status` varchar(20) NOT NULL DEFAULT '已保存' COMMENT '订单状态',
  `remarks` varchar(255) DEFAULT NULL COMMENT '备注',
  `supplier_id` bigint DEFAULT NULL COMMENT '供货商ID',
  `total_amount` decimal(38,2) DEFAULT NULL COMMENT '订单金额',
  `order_no` varchar(255) DEFAULT NULL COMMENT '单据编号',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_purchase_order
-- ----------------------------

-- ----------------------------
-- Table structure for s_purchase_order_item
-- ----------------------------
DROP TABLE IF EXISTS `s_purchase_order_item`;
CREATE TABLE `s_purchase_order_item` (
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
  `subtotal` decimal(38,2) DEFAULT NULL COMMENT '小计 (quantity * unit_price * (1 - discount_value/100) 或 quantity * unit_price - discount_value，根据折扣类型计算)',
  `unit_price` decimal(38,2) DEFAULT NULL COMMENT '单价（以基本单位计）',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_purchase_order_item
-- ----------------------------

-- ----------------------------
-- Table structure for s_purchase_price_record
-- ----------------------------
DROP TABLE IF EXISTS `s_purchase_price_record`;
CREATE TABLE `s_purchase_price_record` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_purchase_price_record
-- ----------------------------

-- ----------------------------
-- Table structure for s_purchase_return
-- ----------------------------
DROP TABLE IF EXISTS `s_purchase_return`;
CREATE TABLE `s_purchase_return` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `approved_at` datetime(6) DEFAULT NULL COMMENT '审核时间',
  `approved_by` bigint DEFAULT NULL COMMENT '审核人',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `merchant_id` bigint NOT NULL,
  `order_status` varchar(20) NOT NULL DEFAULT '已保存' COMMENT '订单状态',
  `purchase_inbound_id` bigint DEFAULT NULL COMMENT '采购入库主表ID',
  `refund_amount` decimal(38,2) DEFAULT NULL COMMENT '退款金额',
  `remarks` varchar(255) DEFAULT NULL COMMENT '备注',
  `return_date` date DEFAULT NULL COMMENT '退单日期',
  `return_reason` varchar(255) DEFAULT NULL COMMENT '退单原因',
  `supplier_id` bigint DEFAULT NULL COMMENT '供货商ID',
  `order_no` varchar(255) DEFAULT NULL COMMENT '单据编号',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_purchase_return
-- ----------------------------

-- ----------------------------
-- Table structure for s_purchase_return_item
-- ----------------------------
DROP TABLE IF EXISTS `s_purchase_return_item`;
CREATE TABLE `s_purchase_return_item` (
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
  `purchase_return_id` bigint DEFAULT NULL COMMENT '采购入库单ID',
  `quantity` double DEFAULT NULL COMMENT '数量（以基本单位计）',
  `return_reason` varchar(255) DEFAULT NULL COMMENT '退货原因',
  `secondary_quantity` double DEFAULT NULL COMMENT '辅助单位数量 (可为空',
  `secondary_unit_id` bigint DEFAULT NULL COMMENT '辅助单位ID(可为空)',
  `subtotal` decimal(38,2) DEFAULT NULL COMMENT '小计 (quantity * unit_price * (1 - discount_value/100) 或 quantity * unit_price - discount_value，根据折扣类型计算)',
  `unit_price` decimal(38,2) DEFAULT NULL COMMENT '单价（以基本单位计）',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `batch_number` varchar(255) DEFAULT NULL COMMENT '批次号',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_purchase_return_item
-- ----------------------------

-- ----------------------------
-- Table structure for s_receipt
-- ----------------------------
DROP TABLE IF EXISTS `s_receipt`;
CREATE TABLE `s_receipt` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_receipt
-- ----------------------------

-- ----------------------------
-- Table structure for s_receipt_item
-- ----------------------------
DROP TABLE IF EXISTS `s_receipt_item`;
CREATE TABLE `s_receipt_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_receipt_item
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

-- ----------------------------
-- Table structure for s_sales_order
-- ----------------------------
DROP TABLE IF EXISTS `s_sales_order`;
CREATE TABLE `s_sales_order` (
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
  `order_status` varchar(20) NOT NULL DEFAULT '已保存' COMMENT '订单状态',
  `remarks` varchar(255) DEFAULT NULL COMMENT '备注',
  `total_amount` decimal(38,2) DEFAULT NULL COMMENT '订单金额',
  `order_no` varchar(255) DEFAULT NULL COMMENT '单据编号',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_sales_order
-- ----------------------------

-- ----------------------------
-- Table structure for s_sales_order_item
-- ----------------------------
DROP TABLE IF EXISTS `s_sales_order_item`;
CREATE TABLE `s_sales_order_item` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_sales_order_item
-- ----------------------------

-- ----------------------------
-- Table structure for s_sales_outbound
-- ----------------------------
DROP TABLE IF EXISTS `s_sales_outbound`;
CREATE TABLE `s_sales_outbound` (
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
  `order_id` bigint DEFAULT NULL COMMENT '销售订单ID',
  `order_status` varchar(20) NOT NULL DEFAULT '已保存' COMMENT '订单状态',
  `outbound_date` datetime(6) DEFAULT NULL COMMENT '出库日期',
  `remarks` varchar(255) DEFAULT NULL COMMENT '备注',
  `total_amount` decimal(38,2) DEFAULT NULL COMMENT '订单金额',
  `order_no` varchar(255) DEFAULT NULL COMMENT '单据编号',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_sales_outbound
-- ----------------------------

-- ----------------------------
-- Table structure for s_sales_outbound_item
-- ----------------------------
DROP TABLE IF EXISTS `s_sales_outbound_item`;
CREATE TABLE `s_sales_outbound_item` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_sales_outbound_item
-- ----------------------------

-- ----------------------------
-- Table structure for s_sales_price_record
-- ----------------------------
DROP TABLE IF EXISTS `s_sales_price_record`;
CREATE TABLE `s_sales_price_record` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_sales_price_record
-- ----------------------------

-- ----------------------------
-- Table structure for s_sales_return
-- ----------------------------
DROP TABLE IF EXISTS `s_sales_return`;
CREATE TABLE `s_sales_return` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `approved_at` datetime(6) DEFAULT NULL COMMENT '审核时间',
  `approved_by` bigint DEFAULT NULL COMMENT '审核人',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `customer_id` bigint DEFAULT NULL COMMENT '客户ID',
  `merchant_id` bigint NOT NULL,
  `order_status` varchar(20) NOT NULL DEFAULT '已保存' COMMENT '订单状态',
  `refund_amount` decimal(38,2) DEFAULT NULL COMMENT '退款金额',
  `remarks` varchar(255) DEFAULT NULL COMMENT '备注',
  `return_date` date DEFAULT NULL COMMENT '退单日期',
  `return_reason` varchar(255) DEFAULT NULL COMMENT '退单原因',
  `sales_outbound_id` bigint DEFAULT NULL COMMENT '销售出库主表ID',
  `order_no` varchar(255) DEFAULT NULL COMMENT '单据编号',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_sales_return
-- ----------------------------

-- ----------------------------
-- Table structure for s_sales_return_item
-- ----------------------------
DROP TABLE IF EXISTS `s_sales_return_item`;
CREATE TABLE `s_sales_return_item` (
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
  `return_reason` varchar(255) DEFAULT NULL COMMENT '退货原因',
  `sales_return_id` bigint DEFAULT NULL COMMENT '销售退单主表ID',
  `secondary_quantity` double DEFAULT NULL COMMENT '辅助单位数量 (可为空',
  `secondary_unit_id` bigint DEFAULT NULL COMMENT '辅助单位ID(可为空)',
  `subtotal` decimal(38,2) DEFAULT NULL COMMENT '小计 (quantity * unitPrice * (1 - discount_value/100) 或 quantity * unitPrice - discount_value，根据折扣类型计算)',
  `unit_price` decimal(38,2) DEFAULT NULL COMMENT '单价（以基本单位计）',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_sales_return_item
-- ----------------------------

-- ----------------------------
-- Table structure for s_settlement_method
-- ----------------------------
DROP TABLE IF EXISTS `s_settlement_method`;
CREATE TABLE `s_settlement_method` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '状态',
  `merchant_id` bigint NOT NULL,
  `name` varchar(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK2avg7rb2ik5r4kfcp7ppxdaq5` (`merchant_id`,`account_book_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_settlement_method
-- ----------------------------

-- ----------------------------
-- Table structure for s_stock_take
-- ----------------------------
DROP TABLE IF EXISTS `s_stock_take`;
CREATE TABLE `s_stock_take` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `approved_at` datetime(6) DEFAULT NULL COMMENT '审核时间',
  `approved_by` bigint DEFAULT NULL COMMENT '审核人',
  `check_date` datetime(6) DEFAULT NULL COMMENT '盘点日期',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `merchant_id` bigint NOT NULL,
  `order_status` varchar(20) NOT NULL DEFAULT '已保存' COMMENT '订单状态',
  `remarks` varchar(255) DEFAULT NULL COMMENT '备注',
  `warehouse_id` bigint DEFAULT NULL COMMENT '仓库ID',
  `order_no` varchar(255) DEFAULT NULL COMMENT '单据编号',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_stock_take
-- ----------------------------

-- ----------------------------
-- Table structure for s_stock_take_item
-- ----------------------------
DROP TABLE IF EXISTS `s_stock_take_item`;
CREATE TABLE `s_stock_take_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `stock_take_id` bigint DEFAULT NULL COMMENT '盘点主表ID',
  `account_book_id` bigint NOT NULL,
  `actual_quantity` int DEFAULT NULL COMMENT '实盘数量',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `difference_reason` varchar(255) DEFAULT NULL COMMENT '备注',
  `merchant_id` bigint NOT NULL,
  `product_id` bigint DEFAULT NULL COMMENT '产品ID',
  `system_quantity` int DEFAULT NULL COMMENT '账面库存',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_stock_take_item
-- ----------------------------

-- ----------------------------
-- Table structure for s_supplier
-- ----------------------------
DROP TABLE IF EXISTS `s_supplier`;
CREATE TABLE `s_supplier` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `code` varchar(255) DEFAULT NULL,
  `contact` varchar(255) DEFAULT NULL COMMENT '联系人',
  `merchant_id` bigint NOT NULL,
  `name` varchar(32) NOT NULL,
  `phone` varchar(255) DEFAULT NULL COMMENT '电话',
  `supplier_category_id` bigint DEFAULT NULL COMMENT '货商分类ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK22r49jgn5b10vxosngqsv11jp` (`merchant_id`,`account_book_id`,`name`)
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
  UNIQUE KEY `UK65c4r889v41h4uiwrk6llth88` (`merchant_id`,`account_book_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_supplier_category
-- ----------------------------

-- ----------------------------
-- Table structure for s_system_config
-- ----------------------------
DROP TABLE IF EXISTS `s_system_config`;
CREATE TABLE `s_system_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account_book_id` bigint NOT NULL,
  `config_type` varchar(255) NOT NULL COMMENT '参数类型',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '状态',
  `merchant_id` bigint NOT NULL,
  `name` varchar(32) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_system_config
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
  UNIQUE KEY `UKnj6xfuqeuvwg8pobbs4s40pi6` (`merchant_id`,`account_book_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_unit
-- ----------------------------

-- ----------------------------
-- Table structure for s_verification
-- ----------------------------
DROP TABLE IF EXISTS `s_verification`;
CREATE TABLE `s_verification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_verification
-- ----------------------------

-- ----------------------------
-- Table structure for s_verification_item
-- ----------------------------
DROP TABLE IF EXISTS `s_verification_item`;
CREATE TABLE `s_verification_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_verification_item
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
  UNIQUE KEY `UKimc9dqlylg45qf23aftyty438` (`merchant_id`,`account_book_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_warehouse
-- ----------------------------
