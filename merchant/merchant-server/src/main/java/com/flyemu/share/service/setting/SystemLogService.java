package com.flyemu.share.service.setting;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.entity.setting.QSystemLog;
import com.flyemu.share.entity.setting.SystemLog;
import com.flyemu.share.repository.SystemLogRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @功能描述: 操作日志
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SystemLogService extends AbsService {

    private final static QSystemLog qSystemLog = QSystemLog.systemLog;

    private final SystemLogRepository systemLogRepository;

    public List<SystemLog> query(Query query) {
        List<SystemLog> systemLogs = bqf.selectFrom(qSystemLog)
                .where(query.builder)
                .orderBy(qSystemLog.id.desc())
                .fetch();
        return systemLogs;
    }

    @Transactional
    public SystemLog save(SystemLog systemLog) {
        if (systemLog.getId() != null) {
            //更新
            SystemLog original = systemLogRepository.getById(systemLog.getId());
            BeanUtil.copyProperties(systemLog, original, CopyOptions.create().ignoreNullValue());
            return systemLogRepository.save(original);
        }
        return systemLogRepository.save(systemLog);
    }

    @Transactional
    public void delete(Long systemLogId, Long merchantId, Long accountBookId) {
        jqf.delete(qSystemLog)
                .where(qSystemLog.id.eq(systemLogId).and(qSystemLog.merchantId.eq(merchantId)).and(qSystemLog.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<SystemLog> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qSystemLog).where(qSystemLog.merchantId.eq(merchantId).and(qSystemLog.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qSystemLog.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qSystemLog.accountBookId.eq(accountBookId));
            }
        }
    }
}
