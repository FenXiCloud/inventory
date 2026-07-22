package com.flyemu.share.service.setting;

import com.flyemu.share.common.TenantAware;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.setting.FinanceItemMap;
import com.flyemu.share.entity.setting.QFinanceItemMap;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.setting.FinanceItemMapRepository;
import com.flyemu.share.service.BaseService;
import com.querydsl.core.BooleanBuilder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FinanceItemMapService extends BaseService {

    private final static QFinanceItemMap qFinanceItemMap = QFinanceItemMap.financeItemMap;

    private final FinanceItemMapRepository financeItemMapRepository;

    public List<FinanceItemMap> query(FinanceItemMapService.Query query) {
        return bqf.selectFrom(qFinanceItemMap)
                .where(query.builder)
                .where(query.builders())
                .orderBy(qFinanceItemMap.id.desc())
                .fetch();
    }

    @Transactional
    public FinanceItemMap save(FinanceItemMap financeItemMap, AccountDto accountDto) {
        if (financeItemMap.getId() != null) {
            //更新
            FinanceItemMap original = financeItemMapRepository.getById(financeItemMap.getId());
            BeanUtil.copyProperties(financeItemMap, original, CopyOptions.create().ignoreNullValue());
            original.setUpdatedAt(LocalDateTime.now());
            return financeItemMapRepository.save(original);
        }
        List<FinanceItemMap> result = financeItemMapRepository.findByInventoryIdAndCategoryId(financeItemMap.getInventoryId(), financeItemMap.getCategoryId());
        if (!result.isEmpty()) {
            throw new ServiceException("已有配置映射~");
        }
        financeItemMap.setCreatedAt(LocalDateTime.now());
        financeItemMap.setCreatedBy(accountDto.getAdminId());
        return financeItemMapRepository.save(financeItemMap);
    }

    @Transactional
    public void delete(Long id, Long merchantId, Long accountBookId) {
        jqf.delete(qFinanceItemMap)
                .where(qFinanceItemMap.id.eq(id).and(qFinanceItemMap.merchantId.eq(merchantId)).and(qFinanceItemMap.accountBookId.eq(accountBookId)))
                .execute();
    }

    public FinanceItemMap load(Long merchantId, Long id) {
        return jqf.selectFrom(qFinanceItemMap)
                .where(qFinanceItemMap.merchantId.eq(merchantId).and(qFinanceItemMap.id.eq(id)))
                .fetchOne();
    }

    public FinanceItemMap findByCategoryIdAndInventoryId(String categoryId, Long inventoryId) {
        List<FinanceItemMap> byCategoryType = financeItemMapRepository.findByCategoryIdAndInventoryId(categoryId, inventoryId);
        if (byCategoryType.isEmpty()) {
            return null;
        }
        return byCategoryType.get(0);
    }

    @Data
    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        private String categoryId;

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qFinanceItemMap.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qFinanceItemMap.accountBookId, accountBookId);
        }

        public BooleanBuilder builders() {
            if (categoryId != null) {
                builder.and(qFinanceItemMap.categoryId.eq(categoryId));
            }
            return builder;
        }

    }
}
