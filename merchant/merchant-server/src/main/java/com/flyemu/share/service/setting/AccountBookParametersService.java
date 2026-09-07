package com.flyemu.share.service.setting;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import com.flyemu.share.entity.setting.AccountBookParameters;
import com.flyemu.share.entity.setting.QAccountBookParameters;
import com.flyemu.share.entity.setting.AccountBook;
import com.flyemu.share.repository.setting.AccountBookParametersRepository;
import com.flyemu.share.service.BaseService;
import com.flyemu.share.service.inventory.CostingMethodSwitchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/** 账套参数设置 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountBookParametersService extends BaseService {
    private static final QAccountBookParameters Q_ACCOUNT_BOOK_PARAMETERS = QAccountBookParameters.accountBookParameters;

    /** 数量/单价小数位上限：与列精度口径一致（数量列可存 4 位；单价扩列脚本后为 6 位），超出会静默截断 */
    private static final int QUANTITY_DECIMAL_MAX = 4;
    private static final int PRICE_DECIMAL_MAX = 6;

    private final AccountBookParametersRepository accountBookParametersRepository;
    private final AccountBookService accountBookService;
    private final CostingMethodSwitchService costingMethodSwitchService;

    @Transactional
    public AccountBookParameters load(Long merchantId, Integer accountBookId) {
        Assert.notNull(accountBookId, "id不能为空");
        AccountBook accountBook = accountBookService.loadById(merchantId, accountBookId.longValue());
        Assert.notNull(accountBook, "账套不存在");
        return findOrCreate(accountBookId);
    }

    @Transactional
    public AccountBookParameters list(Integer id) {
        return findOrCreate(id);
    }

    private AccountBookParameters findOrCreate(Integer accountBookId) {
        Assert.notNull(accountBookId, "id不能为空");
        AccountBookParameters params = bqf
                .selectFrom(Q_ACCOUNT_BOOK_PARAMETERS)
                .where(Q_ACCOUNT_BOOK_PARAMETERS.accountBookId.eq(accountBookId))
                .fetchFirst();
        if (params == null) {
            params = new AccountBookParameters();
            params.setAccountBookId(accountBookId);
            params.setCostAccounting(1);
            params.setAvailableInventory(2);
            params.setQuantityDecimal(2);
            params.setPriceDecimal(2);
            params.setToOrderDefaultStatus("草稿");
            params.setToOrderAutoAudit(false);
            params.setToOrderAllowPartial(true);
            params.setCreateTime(new Date());
            params.setUpdateTime(new Date());
            accountBookParametersRepository.save(params);
        }
        return params;
    }

    /**
     * 保存账套参数：全量合法性校验 + 账套归属校验（防越权），
     * 成本核算方法实际变化时同事务完成批次期初衔接（期间内切换需 costMethodConfirmed 确认）。
     *
     * @return 结果摘要（发生成本法切换时含 affectedCombos 等，供前端 toast）
     */
    @Transactional
    public Dict update(AccountBookParameters accountBookParameters, Long merchantId, Long adminId) {
        Assert.notNull(accountBookParameters.getId(), "id不能为空");
        AccountBookParameters existing = accountBookParametersRepository.findById(accountBookParameters.getId())
                .orElseThrow(() -> new IllegalArgumentException("账套参数不存在"));
        // 归属校验：参数行所属账套必须在当前商户名下（控制器不再强制改写 accountBookId，列表行改非当前账套要落对目标）
        Integer bookId = existing.getAccountBookId();
        Assert.notNull(bookId, "参数行未关联账套");
        AccountBook book = accountBookService.loadById(merchantId, bookId.longValue());
        Assert.notNull(book, "账套不存在或无权修改~");

        Integer originalQuantityDecimal = existing.getQuantityDecimal();
        Integer newPriceDecimal = accountBookParameters.getPriceDecimal();
        Assert.notNull(newPriceDecimal, "单价小数位不能为空");
        Assert.isTrue(newPriceDecimal >= 0 && newPriceDecimal <= PRICE_DECIMAL_MAX,
                "单价小数位必须在 0~" + PRICE_DECIMAL_MAX + " 之间");
        Integer originalPriceDecimal = existing.getPriceDecimal();
        if (originalPriceDecimal != null && newPriceDecimal < originalPriceDecimal) {
            throw new IllegalArgumentException("单价小数位不能由大改小（原值：" + originalPriceDecimal + "）");
        }
        Integer newQuantityDecimal = accountBookParameters.getQuantityDecimal();
        Assert.notNull(newQuantityDecimal, "数量小数位不能为空");
        Assert.isTrue(newQuantityDecimal >= 0 && newQuantityDecimal <= QUANTITY_DECIMAL_MAX,
                "数量小数位必须在 0~" + QUANTITY_DECIMAL_MAX + " 之间");
        if (originalQuantityDecimal != null && newQuantityDecimal < originalQuantityDecimal) {
            throw new IllegalArgumentException("数量小数位不能由大改小（原值：" + originalQuantityDecimal + "）");
        }
        Assert.notNull(accountBookParameters.getCostAccounting(), "成本核算方法不能为空");
        Assert.isTrue(accountBookParameters.getCostAccounting() == 1 || accountBookParameters.getCostAccounting() == 2,
                "成本核算方法取值不合法");
        Assert.notNull(accountBookParameters.getAvailableInventory(), "可用库存允许为负不能为空");
        Assert.isTrue(accountBookParameters.getAvailableInventory() == 1 || accountBookParameters.getAvailableInventory() == 2,
                "可用库存允许为负取值不合法");

        // 成本核算方法切换：确认闸必须在落库前判定，未确认抛错时参数保持原值
        int originalCostAccounting = existing.getCostAccounting() == null ? 1 : existing.getCostAccounting();
        int newCostAccounting = accountBookParameters.getCostAccounting();
        boolean costMethodChanged = newCostAccounting != originalCostAccounting;
        if (costMethodChanged) {
            costingMethodSwitchService.assertConfirmed(merchantId, bookId.longValue(),
                    Boolean.TRUE.equals(accountBookParameters.getCostMethodConfirmed()));
        }

        existing.setCostAccounting(newCostAccounting);
        existing.setAvailableInventory(accountBookParameters.getAvailableInventory());
        existing.setQuantityDecimal(newQuantityDecimal);
        existing.setPriceDecimal(newPriceDecimal);
        // 以销定购参数
        if (accountBookParameters.getToOrderDefaultStatus() != null) {
            existing.setToOrderDefaultStatus(accountBookParameters.getToOrderDefaultStatus());
        }
        if (accountBookParameters.getToOrderAutoAudit() != null) {
            existing.setToOrderAutoAudit(accountBookParameters.getToOrderAutoAudit());
        }
        if (accountBookParameters.getToOrderAllowPartial() != null) {
            existing.setToOrderAllowPartial(accountBookParameters.getToOrderAllowPartial());
        }
        existing.setUpdateTime(new Date());
        accountBookParametersRepository.save(existing);

        if (costMethodChanged) {
            return costingMethodSwitchService.applySwitch(merchantId, bookId.longValue(),
                    originalCostAccounting, newCostAccounting, adminId);
        }
        return Dict.create().set("switched", false);
    }
}
