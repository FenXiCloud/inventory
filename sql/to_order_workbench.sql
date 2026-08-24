-- =========================================================
-- 以销定购工作台 - 菜单与数据模型变更
-- 1. 新增"以销定购看板"菜单（采购管理 → 采购单据）
-- 2. 软废弃 销售预订/进货预订 菜单
-- 3. account_book_parameters 新增以销定购系统参数
-- 4. 新增以销定购操作日志表
-- 5. 商品档案 is_purchasable / default_supplier_id 字段兜底
-- =========================================================

-- 1. 新增菜单：以销定购看板（parent 51 = 采购单据）
INSERT INTO jxc_menu (id, component, enabled, menu_module, menu_type, name, parent_id, pos)
SELECT 261, 'SalesDrivenDashboard', 1, 'MERCHANT', 'MENU', '以销定购看板', 51, 6
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM jxc_menu WHERE id = 261);

-- 2. 授权给所有商户
INSERT INTO jxc_merchant_menu (menu_id, merchant_id)
SELECT 261, id FROM jxc_merchant
WHERE NOT EXISTS (SELECT 1 FROM jxc_merchant_menu WHERE menu_id = 261 AND merchant_id = jxc_merchant.id);

-- 3. 软废弃 销售预订/进货预订（保留数据，隐藏入口）
UPDATE jxc_menu SET enabled = 0 WHERE id IN (257, 258);

-- 4. account_book_parameters 新增以销定购系统参数（幂等）
SET @exist_status := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'jxc_account_book_parameters' AND COLUMN_NAME = 'to_order_default_status');
SET @sql_status := IF(@exist_status = 0, 'ALTER TABLE jxc_account_book_parameters ADD COLUMN to_order_default_status VARCHAR(20) DEFAULT ''待审核'' COMMENT ''以销定购默认单据状态''', 'SELECT 1');
PREPARE stmt_status FROM @sql_status;
EXECUTE stmt_status;
DEALLOCATE PREPARE stmt_status;

SET @exist_audit := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'jxc_account_book_parameters' AND COLUMN_NAME = 'to_order_auto_audit');
SET @sql_audit := IF(@exist_audit = 0, 'ALTER TABLE jxc_account_book_parameters ADD COLUMN to_order_auto_audit BIT(1) DEFAULT 0 COMMENT ''以销定购是否自动审核''', 'SELECT 1');
PREPARE stmt_audit FROM @sql_audit;
EXECUTE stmt_audit;
DEALLOCATE PREPARE stmt_audit;

SET @exist_partial := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'jxc_account_book_parameters' AND COLUMN_NAME = 'to_order_allow_partial');
SET @sql_partial := IF(@exist_partial = 0, 'ALTER TABLE jxc_account_book_parameters ADD COLUMN to_order_allow_partial BIT(1) DEFAULT 1 COMMENT ''以销定购是否允许分批采购''', 'SELECT 1');
PREPARE stmt_partial FROM @sql_partial;
EXECUTE stmt_partial;
DEALLOCATE PREPARE stmt_partial;

-- 存量数据默认值
UPDATE jxc_account_book_parameters SET to_order_default_status = '待审核' WHERE to_order_default_status IS NULL OR to_order_default_status = '';
UPDATE jxc_account_book_parameters SET to_order_auto_audit = 0 WHERE to_order_auto_audit IS NULL;
UPDATE jxc_account_book_parameters SET to_order_allow_partial = 1 WHERE to_order_allow_partial IS NULL;

-- 5. 以销定购操作日志表
CREATE TABLE IF NOT EXISTS `jxc_to_order_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sale_order_id` bigint DEFAULT NULL COMMENT '关联的销售订单ID',
  `purchase_in_id` bigint DEFAULT NULL COMMENT '生成的采购入库单ID',
  `goods_id` bigint DEFAULT NULL COMMENT '商品ID',
  `purchase_quantity` decimal(15,4) DEFAULT NULL COMMENT '本次采购数量',
  `purchase_price` decimal(15,4) DEFAULT NULL COMMENT '本次采购单价',
  `supplier_id` bigint DEFAULT NULL COMMENT '供应商ID',
  `created_by` bigint DEFAULT NULL COMMENT '操作人',
  `created_at` datetime(6) DEFAULT NULL COMMENT '操作时间',
  `account_book_id` bigint NOT NULL COMMENT '账套ID',
  `merchant_id` bigint NOT NULL COMMENT '商户ID',
  PRIMARY KEY (`id`),
  KEY `idx_to_order_log_sale_order` (`sale_order_id`),
  KEY `idx_to_order_log_purchase_in` (`purchase_in_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='以销定购操作日志';

-- 6. 商品档案字段兜底（is_purchasable / default_supplier_id，已存在则跳过）
SET @exist_purchasable := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'jxc_product' AND COLUMN_NAME = 'purchasable');
SET @sql_purchasable := IF(@exist_purchasable = 0, 'ALTER TABLE jxc_product ADD COLUMN purchasable BIT(1) DEFAULT 1 COMMENT ''是否可采购（以销定购筛选）''', 'SELECT 1');
PREPARE stmt_purchasable FROM @sql_purchasable;
EXECUTE stmt_purchasable;
DEALLOCATE PREPARE stmt_purchasable;

SET @exist_supplier := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'jxc_product' AND COLUMN_NAME = 'default_supplier_id');
SET @sql_supplier := IF(@exist_supplier = 0, 'ALTER TABLE jxc_product ADD COLUMN default_supplier_id BIGINT DEFAULT NULL COMMENT ''默认供应商ID''', 'SELECT 1');
PREPARE stmt_supplier FROM @sql_supplier;
EXECUTE stmt_supplier;
DEALLOCATE PREPARE stmt_supplier;
