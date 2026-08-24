-- 货位管理、货位调拨、拣货单菜单SQL
-- 执行方式：mysql -u root -p jxc < location_pick_menu.sql
USE jxc;

-- ==================== 货位管理（挂在基础资料下，parent_id=1）====================

-- 货位管理菜单
INSERT INTO jxc_menu (id, component, name, icon_cls, require_auth, parent_id, enabled, pos, menu_module, menu_type, menu_group)
SELECT 300, 'WarehouseLocationList', '货位管理', NULL, NULL, 1, b'1', 8, 'MERCHANT', 'MENU', 'MERCHANT'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM jxc_menu WHERE id = 300);

-- 说明：parent_id=1 是基础资料菜单的ID

-- ==================== 库存管理新增菜单（挂在库存管理下，parent_id=27）====================

-- 货位调拨菜单
INSERT INTO jxc_menu (id, component, name, icon_cls, require_auth, parent_id, enabled, pos, menu_module, menu_type, menu_group)
SELECT 301, 'LocationTransferList', '货位调拨', NULL, NULL, 27, b'1', 6, 'MERCHANT', 'MENU', 'MERCHANT'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM jxc_menu WHERE id = 301);

-- 拣货单菜单
INSERT INTO jxc_menu (id, component, name, icon_cls, require_auth, parent_id, enabled, pos, menu_module, menu_type, menu_group)
SELECT 302, 'PickOrderList', '拣货单', NULL, NULL, 27, b'1', 7, 'MERCHANT', 'MENU', 'MERCHANT'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM jxc_menu WHERE id = 302);

-- 说明：parent_id=27 是库存管理菜单的ID

-- ==================== 为所有商户授权这些菜单 ====================

INSERT INTO jxc_merchant_menu (menu_id, merchant_id)
SELECT m.id, t.merchant_id
FROM jxc_menu m
JOIN (SELECT DISTINCT merchant_id FROM jxc_merchant_menu) t
WHERE m.id IN (300, 301, 302)
  AND NOT EXISTS (
      SELECT 1 FROM jxc_merchant_menu x
      WHERE x.menu_id = m.id AND x.merchant_id = t.merchant_id
  );

-- ==================== 验证 ====================
SELECT '菜单添加完成' AS status;
SELECT id, name, component, parent_id FROM jxc_menu WHERE id IN (300, 301, 302);
