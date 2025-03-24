package com.flyemu.share.service.setting;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.entity.setting.QSystemConfig;
import com.flyemu.share.entity.setting.SystemConfig;
import com.flyemu.share.repository.SystemConfigRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @功能描述: 系统参数
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SystemConfigService extends AbsService {

    private final static QSystemConfig qSystemConfig = QSystemConfig.systemConfig;

    private final SystemConfigRepository systemConfigRepository;

    public List<SystemConfig> query(Query query) {
        List<SystemConfig> systemConfigs = bqf.selectFrom(qSystemConfig)
                .where(query.builder)
                .orderBy(qSystemConfig.id.desc())
                .fetch();
        return systemConfigs;
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

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qSystemConfig.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qSystemConfig.accountBookId.eq(accountBookId));
            }
        }
    }
}
