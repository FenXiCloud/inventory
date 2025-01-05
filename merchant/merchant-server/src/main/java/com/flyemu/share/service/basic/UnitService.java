package com.flyemu.share.service.basic;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.entity.basic.QUnit;
import com.flyemu.share.entity.basic.Unit;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.UnitRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
/**
 * @功能描述: 单位管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UnitService extends AbsService {

    private final static QUnit qUnit = QUnit.unit;

    private final UnitRepository unitRepository;


    public List<Unit> query(Query query) {
        List<Unit> units = bqf.selectFrom(qUnit)
                .where(query.builder)
                .orderBy(qUnit.id.desc())
                .fetch();
        return units;
    }


    @Transactional
    public Unit save(Unit unit) {
        try {
            if (unit.getId() != null) {
                Unit original = unitRepository.getById(unit.getId());
                BeanUtil.copyProperties(unit, original, CopyOptions.create().ignoreNullValue());
                return unitRepository.save(original);
            }
            return unitRepository.save(unit);
        } catch (Exception e) {
            log.error("Unit", e);
            throw new ServiceException(e.getMessage());
        }
    }


    @Transactional
    public void delete(Long unitsId, Long merchantId, Long accountBookId) {
        jqf.delete(qUnit)
                .where(qUnit.id.eq(unitsId).and(qUnit.merchantId.eq(merchantId)).and(qUnit.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<Unit> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qUnit).where(qUnit.merchantId.eq(merchantId).and(qUnit.accountBookId.eq(accountBookId))).fetch();
    }


    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setFilter(String filter) {
            if (filter != null) {
                builder.and(qUnit.name.contains(filter));
            }
        }

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qUnit.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qUnit.accountBookId.eq(accountBookId));
            }
        }
    }
}
