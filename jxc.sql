/*
Navicat MySQL Data Transfer

Source Server         : local
Source Server Version : 80028
Source Host           : localhost:3306
Source Database       : jxc

Target Server Type    : MYSQL
Target Server Version : 80028
File Encoding         : 65001

Date: 2024-12-30 17:57:00
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
) ENGINE=InnoDB AUTO_INCREMENT=79 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_menu
-- ----------------------------
INSERT INTO `s_menu` VALUES ('1', 'basic', '', 'h-icon-task', 'MERCHANT', 'MERCHANT', 'MENU', '基础资料', null, '0', '');
INSERT INTO `s_menu` VALUES ('6', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '基础资料', '1', '1', '');
INSERT INTO `s_menu` VALUES ('7', 'CustomerList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '客户', '6', '1', '');
INSERT INTO `s_menu` VALUES ('8', 'SupplierList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '供货商', '6', '2', '');
INSERT INTO `s_menu` VALUES ('9', 'WarehouseList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '仓库', '6', '3', '');
INSERT INTO `s_menu` VALUES ('10', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '辅助资料', '1', '3', '');
INSERT INTO `s_menu` VALUES ('12', 'CustomerLevelList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '客户等级', '10', '1', '');
INSERT INTO `s_menu` VALUES ('14', '', '', 'h-icon-setting', 'MERCHANT', 'MERCHANT', 'MENU', '系统设置', null, '10', '');
INSERT INTO `s_menu` VALUES ('15', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '系统设置', '14', '0', '');
INSERT INTO `s_menu` VALUES ('16', 'MerchantInfo', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '企业信息', '15', '0', '');
INSERT INTO `s_menu` VALUES ('17', 'AccountBookList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '账套管理', '15', '1', '');
INSERT INTO `s_menu` VALUES ('18', 'RoleList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '角色权限', '15', '2', '');
INSERT INTO `s_menu` VALUES ('19', 'AdminList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '员工账号', '15', '3', '');
INSERT INTO `s_menu` VALUES ('20', 'ProductList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '产品', '6', '0', '');
INSERT INTO `s_menu` VALUES ('21', 'UnitList', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '计量单位', '10', '1', '');
INSERT INTO `s_menu` VALUES ('22', 'Account', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '账户管理', '10', '2', '');
INSERT INTO `s_menu` VALUES ('23', 'AccountType', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '收支类型', '10', '3', '');
INSERT INTO `s_menu` VALUES ('24', 'SettlementMethod', '', null, 'MERCHANT', 'MERCHANT', 'MENU', '结算方式', '10', '4', '');
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
INSERT INTO `s_menu` VALUES ('43', null, '', null, 'MERCHANT', 'MERCHANT', 'MENU', '价格设置', '1', '2', '');
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
INSERT INTO `s_merchant` VALUES ('1', '杭州', '32f90df1-c739-463b-8218-715437765807', '2024-12-29 18:54:06.747190', null, '', '李泽龙', '13944878765', '纷析云', '2024-12-01', '2024-12-01');

-- ----------------------------
-- Table structure for s_merchant_menu
-- ----------------------------
DROP TABLE IF EXISTS `s_merchant_menu`;
CREATE TABLE `s_merchant_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `menu_id` bigint NOT NULL,
  `merchant_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=156 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of s_merchant_menu
-- ----------------------------
INSERT INTO `s_merchant_menu` VALUES ('84', '1', '1');
INSERT INTO `s_merchant_menu` VALUES ('85', '6', '1');
INSERT INTO `s_merchant_menu` VALUES ('86', '7', '1');
INSERT INTO `s_merchant_menu` VALUES ('87', '8', '1');
INSERT INTO `s_merchant_menu` VALUES ('88', '9', '1');
INSERT INTO `s_merchant_menu` VALUES ('89', '10', '1');
INSERT INTO `s_merchant_menu` VALUES ('90', '12', '1');
INSERT INTO `s_merchant_menu` VALUES ('91', '14', '1');
INSERT INTO `s_merchant_menu` VALUES ('92', '15', '1');
INSERT INTO `s_merchant_menu` VALUES ('93', '16', '1');
INSERT INTO `s_merchant_menu` VALUES ('94', '17', '1');
INSERT INTO `s_merchant_menu` VALUES ('95', '18', '1');
INSERT INTO `s_merchant_menu` VALUES ('96', '19', '1');
INSERT INTO `s_merchant_menu` VALUES ('97', '20', '1');
INSERT INTO `s_merchant_menu` VALUES ('98', '21', '1');
INSERT INTO `s_merchant_menu` VALUES ('99', '22', '1');
INSERT INTO `s_merchant_menu` VALUES ('100', '23', '1');
INSERT INTO `s_merchant_menu` VALUES ('101', '24', '1');
INSERT INTO `s_merchant_menu` VALUES ('102', '25', '1');
INSERT INTO `s_merchant_menu` VALUES ('103', '26', '1');
INSERT INTO `s_merchant_menu` VALUES ('104', '27', '1');
INSERT INTO `s_merchant_menu` VALUES ('105', '28', '1');
INSERT INTO `s_merchant_menu` VALUES ('106', '29', '1');
INSERT INTO `s_merchant_menu` VALUES ('107', '30', '1');
INSERT INTO `s_merchant_menu` VALUES ('108', '31', '1');
INSERT INTO `s_merchant_menu` VALUES ('109', '32', '1');
INSERT INTO `s_merchant_menu` VALUES ('110', '33', '1');
INSERT INTO `s_merchant_menu` VALUES ('111', '34', '1');
INSERT INTO `s_merchant_menu` VALUES ('112', '35', '1');
INSERT INTO `s_merchant_menu` VALUES ('113', '36', '1');
INSERT INTO `s_merchant_menu` VALUES ('114', '37', '1');
INSERT INTO `s_merchant_menu` VALUES ('115', '38', '1');
INSERT INTO `s_merchant_menu` VALUES ('116', '39', '1');
INSERT INTO `s_merchant_menu` VALUES ('117', '40', '1');
INSERT INTO `s_merchant_menu` VALUES ('118', '41', '1');
INSERT INTO `s_merchant_menu` VALUES ('119', '42', '1');
INSERT INTO `s_merchant_menu` VALUES ('120', '43', '1');
INSERT INTO `s_merchant_menu` VALUES ('121', '44', '1');
INSERT INTO `s_merchant_menu` VALUES ('122', '45', '1');
INSERT INTO `s_merchant_menu` VALUES ('123', '46', '1');
INSERT INTO `s_merchant_menu` VALUES ('124', '47', '1');
INSERT INTO `s_merchant_menu` VALUES ('125', '48', '1');
INSERT INTO `s_merchant_menu` VALUES ('126', '49', '1');
INSERT INTO `s_merchant_menu` VALUES ('127', '50', '1');
INSERT INTO `s_merchant_menu` VALUES ('128', '51', '1');
INSERT INTO `s_merchant_menu` VALUES ('129', '52', '1');
INSERT INTO `s_merchant_menu` VALUES ('130', '53', '1');
INSERT INTO `s_merchant_menu` VALUES ('131', '54', '1');
INSERT INTO `s_merchant_menu` VALUES ('132', '55', '1');
INSERT INTO `s_merchant_menu` VALUES ('133', '56', '1');
INSERT INTO `s_merchant_menu` VALUES ('134', '57', '1');
INSERT INTO `s_merchant_menu` VALUES ('135', '58', '1');
INSERT INTO `s_merchant_menu` VALUES ('136', '59', '1');
INSERT INTO `s_merchant_menu` VALUES ('137', '60', '1');
INSERT INTO `s_merchant_menu` VALUES ('138', '61', '1');
INSERT INTO `s_merchant_menu` VALUES ('139', '62', '1');
INSERT INTO `s_merchant_menu` VALUES ('140', '63', '1');
INSERT INTO `s_merchant_menu` VALUES ('141', '64', '1');
INSERT INTO `s_merchant_menu` VALUES ('142', '65', '1');
INSERT INTO `s_merchant_menu` VALUES ('143', '66', '1');
INSERT INTO `s_merchant_menu` VALUES ('144', '67', '1');
INSERT INTO `s_merchant_menu` VALUES ('145', '68', '1');
INSERT INTO `s_merchant_menu` VALUES ('146', '69', '1');
INSERT INTO `s_merchant_menu` VALUES ('147', '70', '1');
INSERT INTO `s_merchant_menu` VALUES ('148', '71', '1');
INSERT INTO `s_merchant_menu` VALUES ('149', '72', '1');
INSERT INTO `s_merchant_menu` VALUES ('150', '73', '1');
INSERT INTO `s_merchant_menu` VALUES ('151', '74', '1');
INSERT INTO `s_merchant_menu` VALUES ('152', '75', '1');
INSERT INTO `s_merchant_menu` VALUES ('153', '76', '1');
INSERT INTO `s_merchant_menu` VALUES ('154', '77', '1');
INSERT INTO `s_merchant_menu` VALUES ('155', '78', '1');

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
INSERT INTO `s_merchant_user` VALUES ('1', '', '2024-12-30 15:27:24.721899', '李泽龙', '$2a$10$MPZ4oROTiWvlKqiq79Sk7.TMcY3D8p8edgUpO5UarsCMroUeChSQC', '', '13944878765');

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
