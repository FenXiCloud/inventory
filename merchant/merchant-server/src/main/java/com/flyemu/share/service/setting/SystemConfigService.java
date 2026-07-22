package com.flyemu.share.service.setting;

import com.flyemu.share.common.TenantAware;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.entity.setting.QSystemConfig;
import com.flyemu.share.entity.setting.SystemConfig;
import com.flyemu.share.repository.setting.SystemConfigRepository;
import com.flyemu.share.service.BaseService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SystemConfigService extends BaseService {

    private final static QSystemConfig qSystemConfig = QSystemConfig.systemConfig;

    private final SystemConfigRepository systemConfigRepository;

    public List<SystemConfig> query(Query query) {
        return bqf.selectFrom(qSystemConfig)
                .where(query.builder)
                .orderBy(qSystemConfig.id.desc())
                .fetch();
    }

    @Transactional
    public SystemConfig save(SystemConfig systemConfig) {
        if (systemConfig.getId() != null) {
            //更新
            SystemConfig original = systemConfigRepository.getById(systemConfig.getId());
            BeanUtil.copyProperties(systemConfig, original, CopyOptions.create().ignoreNullValue());
            return systemConfigRepository.save(original);
        }
        return systemConfigRepository.save(systemConfig);
    }

    @Transactional
    public void delete(Long systemConfigId, Long merchantId, Long accountBookId) {
        jqf.delete(qSystemConfig)
                .where(qSystemConfig.id.eq(systemConfigId).and(qSystemConfig.merchantId.eq(merchantId)).and(qSystemConfig.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<SystemConfig> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qSystemConfig).where(qSystemConfig.merchantId.eq(merchantId).and(qSystemConfig.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qSystemConfig.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qSystemConfig.accountBookId, accountBookId);
        }
    }
}
