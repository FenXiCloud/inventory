package com.flyemu.share.service.setting;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.api.FenxiLogin;
import com.flyemu.share.entity.setting.FinanceRel;
import com.flyemu.share.entity.setting.QFinanceRel;
import com.flyemu.share.repository.FinanceRelRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * @功能描述: 关联云财务
 * @创建时间: 2025年03月11日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FinanceRelService extends AbsService {

    private final static QFinanceRel qFinanceRel = QFinanceRel.financeRel;

    private final FinanceRelRepository financeRelRepository;

    private final FenxiLogin fenxiLogin;

    public List<FinanceRel> query(Query query) {
        List<FinanceRel> financeRels = bqf.selectFrom(qFinanceRel)
                .where(query.builder)
                .orderBy(qFinanceRel.id.desc())
                .fetch();
        return financeRels;
    }


    @Transactional
    public FinanceRel save(FinanceRel financeRel) {
        if (financeRel.getId() != null) {
            //更新
            FinanceRel original = financeRelRepository.getById(financeRel.getId());
            BeanUtil.copyProperties(financeRel, original, CopyOptions.create().ignoreNullValue());
            return financeRelRepository.save(original);
        }

        return financeRelRepository.save(financeRel);
    }

    @Transactional
    public FinanceRel upCookie(Long merchantId, Long accountBookId) {
        FinanceRel relationAccount = bqf.selectFrom(qFinanceRel)
                .where(qFinanceRel.merchantId.eq(merchantId).and(qFinanceRel.accountBookId.eq(accountBookId))).fetchFirst();
        String cookie = fenxiLogin.getCookie(relationAccount);
        jqf.update(qFinanceRel)
                .set(qFinanceRel.cookie, cookie)
                .where(qFinanceRel.id.eq(relationAccount.getId())).execute();
        relationAccount.setCookie(cookie);
        return relationAccount;
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qFinanceRel.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qFinanceRel.accountBookId.eq(accountBookId));
            }
        }

    }
}
