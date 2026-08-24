-- 以销定购模块建表SQL
-- 执行方式: mysql -u root -p jxc < sales_reservation_tables.sql

-- 销售预订主表
CREATE TABLE IF NOT EXISTS `jxc_sales_reservation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `merchant_id` bigint NOT NULL,
  `account_book_id` bigint NOT NULL,
  `order_no` varchar(50) NOT NULL,
  `customer_id` bigint NOT NULL,
  `order_date` date NOT NULL,
  `total_amount` decimal(18,2) DEFAULT '0.00',
  `discount_amount` decimal(18,2) DEFAULT '0.00',
  `final_amount` decimal(18,2) DEFAULT '0.00',
  `discount_rate` decimal(10,2) DEFAULT '0.00',
  `remarks` varchar(500) DEFAULT NULL,
  `order_status` varchar(20) NOT NULL DEFAULT '已保存',
  `status` int NOT NULL DEFAULT '0' COMMENT '0=初始 1=部分转进货 2=全部转进货',
  `sales_order_id` bigint DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `approved_by` bigint DEFAULT NULL,
  `approved_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_merchant_account` (`merchant_id`, `account_book_id`),
  KEY `idx_customer` (`customer_id`),
  KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 销售预订明细表
CREATE TABLE IF NOT EXISTS `jxc_sales_reservation_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `merchant_id` bigint NOT NULL,
  `account_book_id` bigint NOT NULL,
  `sales_reservation_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `base_unit_id` bigint NOT NULL,
  `quantity` decimal(18,2) NOT NULL DEFAULT '0.00',
  `quantity_purchased` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '已转进货数量',
  `secondary_unit_id` bigint DEFAULT NULL,
  `secondary_quantity` decimal(18,2) DEFAULT '0.00',
  `conversion_rate` decimal(10,4) DEFAULT '1.0000',
  `unit_price` decimal(18,2) DEFAULT '0.00',
  `discount_rate` decimal(10,2) DEFAULT '0.00',
  `discount_value` decimal(18,2) DEFAULT '0.00',
  `subtotal` decimal(18,2) DEFAULT '0.00',
  `warehouse_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_reservation_id` (`sales_reservation_id`),
  KEY `idx_merchant_account` (`merchant_id`, `account_book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 进货预订主表
CREATE TABLE IF NOT EXISTS `jxc_purchase_reservation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `merchant_id` bigint NOT NULL,
  `account_book_id` bigint NOT NULL,
  `order_no` varchar(50) NOT NULL,
  `supplier_id` bigint NOT NULL,
  `order_date` date NOT NULL,
  `total_amount` decimal(18,2) DEFAULT '0.00',
  `discount_rate` decimal(10,2) DEFAULT '0.00',
  `discount_amount` decimal(18,2) DEFAULT '0.00',
  `final_amount` decimal(18,2) DEFAULT '0.00',
  `remarks` varchar(500) DEFAULT NULL,
  `order_status` varchar(20) NOT NULL DEFAULT '已保存',
  `status` int NOT NULL DEFAULT '0' COMMENT '0=初始 1=部分转采购 2=全部转采购',
  `purchase_order_id` bigint DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `approved_by` bigint DEFAULT NULL,
  `approved_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_merchant_account` (`merchant_id`, `account_book_id`),
  KEY `idx_supplier` (`supplier_id`),
  KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 进货预订明细表
CREATE TABLE IF NOT EXISTS `jxc_purchase_reservation_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `merchant_id` bigint NOT NULL,
  `account_book_id` bigint NOT NULL,
  `purchase_reservation_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `base_unit_id` bigint NOT NULL,
  `quantity` decimal(18,2) NOT NULL DEFAULT '0.00',
  `quantity_ordered` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '已转采购数量',
  `secondary_unit_id` bigint DEFAULT NULL,
  `secondary_quantity` decimal(18,2) DEFAULT '0.00',
  `conversion_rate` decimal(10,4) DEFAULT '1.0000',
  `unit_price` decimal(18,2) DEFAULT '0.00',
  `discount_rate` decimal(10,2) DEFAULT '0.00',
  `discount_value` decimal(18,2) DEFAULT '0.00',
  `subtotal` decimal(18,2) DEFAULT '0.00',
  `warehouse_id` bigint DEFAULT NULL,
  `sales_reservation_id` bigint DEFAULT NULL,
  `sales_reservation_item_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_purchase_reservation_id` (`purchase_reservation_id`),
  KEY `idx_merchant_account` (`merchant_id`, `account_book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
