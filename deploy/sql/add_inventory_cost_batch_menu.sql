-- 库存管理 / 库存报表 / 库存批次表
-- 顺序：库存余额表(0) → 库存批次表(1) → 进销存明细表(2) → 进销存汇总表(3)

INSERT INTO jxc_menu (
  id, component, name, icon_cls, parent_id, pos,
  enabled, menu_type, menu_module, menu_group, require_auth
) VALUES (
  87, 'InventoryCostBatchList', '库存批次表', NULL, 68, 1,
  b'1', 'MENU', 'MERCHANT', 'MERCHANT', NULL
)
ON DUPLICATE KEY UPDATE
  component = VALUES(component),
  name = VALUES(name),
  parent_id = VALUES(parent_id),
  pos = VALUES(pos),
  enabled = VALUES(enabled),
  menu_type = VALUES(menu_type),
  menu_module = VALUES(menu_module),
  menu_group = VALUES(menu_group);

UPDATE jxc_menu SET name = '库存批次表', pos = 1 WHERE id = 87;
UPDATE jxc_menu SET pos = 2 WHERE id = 73;
UPDATE jxc_menu SET pos = 3 WHERE id = 74;

INSERT INTO jxc_merchant_menu (menu_id, merchant_id)
SELECT 87, m.id
FROM jxc_merchant m
WHERE NOT EXISTS (
  SELECT 1
  FROM jxc_merchant_menu mm
  WHERE mm.menu_id = 87
    AND mm.merchant_id = m.id
);
