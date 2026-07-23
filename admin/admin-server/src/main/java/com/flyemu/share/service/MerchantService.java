package com.flyemu.share.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.setting.*;
import com.flyemu.share.repository.setting.*;
import com.querydsl.core.BooleanBuilder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MerchantService extends BaseService {

    private static final QMerchant qMerchant = QMerchant.merchant;

    private static final QMenu qMenu = QMenu.menu;

    private final MerchantRepository merchantRepository;

    private final AdminRepository adminRepository;

    private final RoleRepository roleRepository;

    private final AccountBookRepository accountBookRepository;

    private final MenuRepository menuRepository;

    private final MerchantMenuRepository merchantMenuRepository;

    @PostConstruct
    public void initDefaultUser() {
        ensureDefaultMenus();
        Long count = jqf.selectFrom(qMerchant).select(qMerchant.count()).fetchFirst();
        if (count == 0) {
            Merchant merchant = new Merchant();
            merchant.setCode(UUID.randomUUID().toString());
            merchant.setName("纷析云");
            merchant.setAddress("杭州");
            merchant.setCreatedAt(LocalDateTime.now());
            merchant.setContact("李泽龙");
            merchant.setMobile("13944878765");
            merchant.setEnabled(true);
            this.save(merchant);

            // 初始化菜单数据
            initMenus(merchant.getId());

            log.info("测试商户账号：13944878765，密码：878765");
        } else {
            // 已有商户：补齐新增菜单关联
            List<Long> merchantIds = jqf.selectFrom(qMerchant).select(qMerchant.id).fetch();
            for (Long merchantId : merchantIds) {
                associateMissingMenus(merchantId);
            }
        }
    }

    public PageResults<Merchant> query(Page page, Query query) {
        PagedList<Merchant> fetchPage = bqf.selectFrom(qMerchant)
                .where(query.builder)
                .orderBy(qMerchant.enabled.desc(), qMerchant.id.desc())
                .fetchPage(page.getOffset(), page.getOffsetEnd());
        return new PageResults<>(fetchPage, page);
    }

    @Transactional
    public Merchant save(Merchant merchant) {
        if (merchant.getId() != null) {
            //更新
            Merchant original = merchantRepository.getById(merchant.getId());
            BeanUtil.copyProperties(merchant, original, CopyOptions.create().ignoreNullValue());
            return merchantRepository.save(original);
        } else {
            Assert.notBlank(merchant.getName(), "商户名称不允许为空~");
            Assert.notBlank(merchant.getContact(), "联系人不允许为空~");
            Assert.notBlank(merchant.getMobile(), "联系人电话不允许为空~");
            String mobile = merchant.getMobile().trim();
            Assert.isTrue(mobile.length() >= 6, "联系人电话格式不正确~");
            merchant.setMobile(mobile);

            merchant.setEnabled(true);
            merchant.setCreatedAt(LocalDateTime.now());
            if (StrUtil.isBlank(merchant.getCode())) {
                merchant.setCode(UUID.randomUUID().toString());
            }
            merchantRepository.save(merchant);

            //创建默认角色和管理员账号（初始密码为手机号后6位）
            Role role = new Role();
            role.setName("商户管理员");
            role.setSystemDefault(true);
            role.setMerchantId(merchant.getId());
            roleRepository.save(role);

            Admin admin = new Admin();
            admin.setName(merchant.getContact());
            admin.setMobile(mobile);
            admin.setUsername(mobile);
            admin.setPassword(DigestUtil.bcrypt(mobile.substring(mobile.length() - 6)));
            admin.setEnabled(true);
            admin.setMerchantId(merchant.getId());
            admin.setSystemDefault(true);
            admin.setRoleId(role.getId());
            adminRepository.save(admin);

            // 创建默认账套
            AccountBook accountBook = new AccountBook();
            accountBook.setMerchantId(merchant.getId());
            accountBook.setCurrent(true);
            accountBook.setName(merchant.getName());
            accountBook.setEnabled(true);
            accountBookRepository.save(accountBook);

            // 为新商户关联菜单
            associateMerchantMenus(merchant.getId());
        }
        return merchantRepository.save(merchant);
    }

    @Transactional
    public void delete(Long merchantId) {
        merchantRepository.deleteById(merchantId);
    }

    public List<Merchant> select() {
        return bqf.selectFrom(qMerchant).orderBy(qMerchant.code.asc()).fetch();
    }

    /**
     * 初始化菜单数据（仅首次启动时全量写入；后续由 ensureDefaultMenus 补齐）
     */
    private void initMenus(Long merchantId) {
        ensureDefaultMenus();
        associateMerchantMenus(merchantId);
    }

    /**
     * 按默认菜单清单补齐缺失菜单（已有库升级用）
     */
    @Transactional
    public void ensureDefaultMenus() {
        List<Menu> defaults = buildDefaultMenus();
        int added = 0;
        for (Menu menu : defaults) {
            Long exists = jqf.selectFrom(qMenu).select(qMenu.count())
                    .where(qMenu.id.eq(menu.getId()))
                    .fetchFirst();
            if (exists == null || exists == 0) {
                menuRepository.save(menu);
                added++;
            }
        }
        if (added > 0) {
            log.info("补齐菜单数据完成，新增 {} 条", added);
        }
    }

    /**
     * 为新商户关联所有菜单
     */
    @Transactional
    public void associateMerchantMenus(Long merchantId) {
        associateMissingMenus(merchantId);
    }

    /**
     * 仅为商户补齐尚未关联的菜单，避免重复插入
     */
    @Transactional
    public void associateMissingMenus(Long merchantId) {
        QMerchantMenu qMerchantMenu = QMerchantMenu.merchantMenu;
        List<Long> existing = jqf.selectFrom(qMerchantMenu)
                .select(qMerchantMenu.menuId)
                .where(qMerchantMenu.merchantId.eq(merchantId))
                .fetch();
        Set<Long> existingSet = new HashSet<>(existing);
        List<Long> menuIds = jqf.selectFrom(qMenu).select(qMenu.id).fetch();
        List<MerchantMenu> merchantMenus = menuIds.stream()
                .filter(menuId -> !existingSet.contains(menuId))
                .map(menuId -> {
                    MerchantMenu mm = new MerchantMenu();
                    mm.setMenuId(menuId);
                    mm.setMerchantId(merchantId);
                    return mm;
                })
                .collect(Collectors.toList());
        if (merchantMenus.isEmpty()) {
            return;
        }
        merchantMenuRepository.saveAll(merchantMenus);
        log.info("为商户 {} 补齐菜单关联 {} 条", merchantId, merchantMenus.size());
    }

    /**
     * 构建默认菜单数据
     */
    private List<Menu> buildDefaultMenus() {
        List<Menu> menus = new ArrayList<>();

        // id=1: 基础资料 (根菜单)
        menus.add(menu(1L, "basic", "基础资料", "root-list", null, 0));
        // id=6: 基础资料 (子分类)
        menus.add(menu(6L, null, "基础资料", null, 1L, 1));
        // id=7: 客户档案
        menus.add(menu(7L, "CustomerList", "客户档案", null, 6L, 1));
        // id=20: 产品档案
        menus.add(menu(20L, "ProductList", "产品档案", null, 6L, 0));
        // id=8: 货商档案
        menus.add(menu(8L, "SupplierList", "货商档案", null, 6L, 2));
        // id=9: 仓库管理
        menus.add(menu(9L, "WarehouseList", "仓库管理", null, 6L, 3));

        // id=10: 辅助资料 (子分类)
        menus.add(menu(10L, null, "辅助资料", null, 1L, 2));
        // id=12: 客户等级
        menus.add(menu(12L, "CustomerLevelList", "客户等级", null, 10L, 1));
        // id=79: 账户管理
        menus.add(menu(79L, "AccountList", "账户管理", null, 10L, 2));
        // id=80: 计量单位
        menus.add(menu(80L, "UnitList", "计量单位", null, 10L, 3));
        // id=23: 收支类型
        menus.add(menu(23L, "AccountTypeList", "收支类型", null, 10L, 4));
        // id=24: 结算方式
        menus.add(menu(24L, "PaymentMethodList", "结算方式", null, 10L, 5));

        // id=43: 价格设置 (子分类)
        menus.add(menu(43L, null, "价格设置", null, 1L, 3));
        // id=44: 产品价格资料
        menus.add(menu(44L, "ProductPriceList", "产品价格资料", null, 43L, 0));
        // id=45: 产品价格记录
        menus.add(menu(45L, "PriceRecordList", "产品价格记录", null, 43L, 1));
        // id=81: 价格取数规则
        menus.add(menu(81L, "PricingPolicyList", "价格取数规则", null, 43L, 2));

        // id=82: 期初录入 (子分类)
        menus.add(menu(82L, null, "期初录入", null, 1L, 4));
        // id=83: 库存期初录入
        menus.add(menu(83L, "InventoryInitialList", "库存期初录入", null, 82L, 0));
        // id=84: 客户期初录入
        menus.add(menu(84L, "CustomerInitialList", "客户期初录入", null, 82L, 1));
        // id=85: 货商期初录入
        menus.add(menu(85L, "SupplierInitialList", "货商期初录入", null, 82L, 2));

        // id=14: 系统设置 (根菜单)
        menus.add(menu(14L, "Setting", "系统设置", "setting", null, 10));
        // id=15: 系统设置 (子分类)
        menus.add(menu(15L, null, "系统设置", null, 14L, 0));
        // id=16: 企业信息
        menus.add(menu(16L, "MerchantInfo", "企业信息", null, 15L, 0));
        // id=17: 账套管理
        menus.add(menu(17L, "AccountBookList", "账套管理", null, 15L, 1));
        // id=18: 角色权限
        menus.add(menu(18L, "RoleList", "角色权限", null, 15L, 2));
        // id=19: 员工账号
        menus.add(menu(19L, "AdminList", "员工账号", null, 15L, 3));
        // id=46: 编码规则
        menus.add(menu(46L, "CodeRuleList", "编码规则", null, 15L, 4));
        // id=47: 结账/反结账
        menus.add(menu(47L, "CheckoutList", "结账/反结账", null, 15L, 5));
        // id=48: 打印模板
        menus.add(menu(48L, "PrintTemplateList", "打印模板", null, 15L, 6));
        // id=49: 备份与恢复
        menus.add(menu(49L, "BackupRestore", "备份与恢复", null, 15L, 7));
        // id=50: 操作日志
        menus.add(menu(50L, "SystemLogList", "操作日志", null, 15L, 8));

        // id=25: 采购管理 (根菜单)
        menus.add(menu(25L, "Purchase", "采购管理", "cart", null, 1));
        // id=51: 采购单据 (子分类)
        menus.add(menu(51L, null, "采购单据", null, 25L, 0));
        // id=52: 采购订单
        menus.add(menu(52L, "PurchaseOrderList", "采购订单", null, 51L, 0));
        // id=53: 采购入库单
        menus.add(menu(53L, "PurchaseInboundList", "采购入库单", null, 51L, 1));
        // id=54: 采购退货单
        menus.add(menu(54L, "PurchaseReturnList", "采购退货单", null, 51L, 2));
        // id=55: 采购报表 (子分类)
        menus.add(menu(55L, null, "采购报表", null, 25L, 1));
        // id=56: 采购明细表
        menus.add(menu(56L, "PurchaseItemReport", "采购明细表", null, 55L, 0));
        // id=57: 采购汇总表
        menus.add(menu(57L, "PurchaseSummaryReport", "采购汇总表", null, 55L, 1));

        // id=26: 销售管理 (根菜单)
        menus.add(menu(26L, "Sales", "销售管理", "shop", null, 2));
        // id=58: 销售单据 (子分类)
        menus.add(menu(58L, null, "销售单据", null, 26L, 0));
        // id=60: 销售订单
        menus.add(menu(60L, "SalesOrderList", "销售订单", null, 58L, 0));
        // id=61: 销售出库单
        menus.add(menu(61L, "SalesOutboundList", "销售出库单", null, 58L, 1));
        // id=62: 销售退货单
        menus.add(menu(62L, "SalesReturnList", "销售退货单", null, 58L, 2));
        // id=59: 销售报表 (子分类)
        menus.add(menu(59L, null, "销售报表", null, 26L, 1));
        // id=63: 销售明细表
        menus.add(menu(63L, "SalesItemReport", "销售明细表", null, 59L, 0));
        // id=64: 销售汇总表
        menus.add(menu(64L, "SalesSummaryReport", "销售汇总表", null, 59L, 1));
        // id=65: 销售利润表
        menus.add(menu(65L, "SalesProfitReport", "销售利润表", null, 59L, 2));
        // id=66: 销售排行表
        menus.add(menu(66L, "SalesRankingReport", "销售排行表", null, 59L, 3));

        // id=27: 库存管理 (根菜单)
        menus.add(menu(27L, "Inventory", "库存管理", "layers", null, 3));
        // id=67: 库存单据 (子分类)
        menus.add(menu(67L, null, "库存单据", null, 27L, 0));
        // id=75: 调拨单
        menus.add(menu(75L, "InventoryTransferList", "调拨单", null, 67L, 0));
        // id=76: 盘点单
        menus.add(menu(76L, "StockTakeList", "盘点单", null, 67L, 1));
        // id=77: 其他出库单
        menus.add(menu(77L, "OtherOutboundList", "其他出库单", null, 67L, 2));
        // id=78: 其他入库单
        menus.add(menu(78L, "OtherInboundList", "其他入库单", null, 67L, 3));
        // id=86: 成本调整单
        menus.add(menu(86L, "CostAdjustmentList", "成本调整单", null, 67L, 5));
        // id=68: 库存报表 (子分类)
        menus.add(menu(68L, null, "库存报表", null, 27L, 1));
        // id=72: 库存余额表
        menus.add(menu(72L, "InventoryReport", "库存余额表", null, 68L, 0));
        // id=87: 库存批次表
        menus.add(menu(87L, "InventoryCostBatchList", "库存批次表", null, 68L, 1));
        // id=73: 进销存明细表
        menus.add(menu(73L, "InventoryItemReport", "进销存明细表", null, 68L, 2));
        // id=74: 进销存汇总表
        menus.add(menu(74L, "InventorySummaryReport", "进销存汇总表", null, 68L, 3));

        // id=28: 资金账户 (根菜单)
        menus.add(menu(28L, "Fund", "资金账户", "wallet", null, 4));
        // id=29: 资金单据 (子分类)
        menus.add(menu(29L, null, "资金单据", null, 28L, 0));
        // id=30: 收款单
        menus.add(menu(30L, "OrderReceiptList", "收款单", null, 29L, 0));
        // id=31: 付款单
        menus.add(menu(31L, "OrderPaymentList", "付款单", null, 29L, 1));
        // id=32: 核销单
        menus.add(menu(32L, "VerificationList", "核销单", null, 29L, 2));
        // id=33: 资金转账
        menus.add(menu(33L, "AccountTransferList", "资金转账", null, 29L, 3));
        // id=34: 其他收入单
        menus.add(menu(34L, "OtherReceiptList", "其他收入单", null, 29L, 4));
        // id=35: 其他支出单
        menus.add(menu(35L, "OtherExpenseList", "其他支出单", null, 29L, 5));
        // id=36: 资金报表 (子分类)
        menus.add(menu(36L, null, "资金报表", null, 28L, 1));
        // id=37: 往来单位欠款表
        menus.add(menu(37L, "CounterpartDebt", "往来单位欠款表", null, 36L, 0));
        // id=38: 客户对账单
        menus.add(menu(38L, "CustomerStatements", "客户对账单", null, 36L, 1));
        // id=39: 供货商对账单
        menus.add(menu(39L, "VendorStatements", "供货商对账单", null, 36L, 2));
        // id=40: 应收账款明细表
        menus.add(menu(40L, "CustomerFlowReport", "应收账款明细表", null, 36L, 3));
        // id=41: 应付账款明细表
        menus.add(menu(41L, "SupplierFlowReport", "应付账款明细表", null, 36L, 4));
        // id=42: 其他收支明细表
        menus.add(menu(42L, "OtherIncomeExpenseReport", "其他收支明细表", null, 36L, 5));

        return menus;
    }

    private Menu menu(Long id, String component, String name, String iconCls, Long parentId, Integer pos) {
        Menu menu = new Menu();
        menu.setId(id);
        menu.setComponent(component);
        menu.setName(name);
        menu.setIconCls(iconCls);
        menu.setParentId(parentId);
        menu.setPos(pos);
        menu.setEnabled(true);
        menu.setMenuType(Menu.MenuType.MENU);
        menu.setMenuModule(Menu.MenuModule.MERCHANT);
        menu.setMenuGroup(Menu.MenuGroup.MERCHANT);
        return menu;
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setName(String name) {
            if (StrUtil.isNotEmpty(name)) {
                builder.and(qMerchant.name.contains(name));
            }
        }
    }
}
