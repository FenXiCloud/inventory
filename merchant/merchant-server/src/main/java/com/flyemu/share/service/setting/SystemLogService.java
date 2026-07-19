package com.flyemu.share.service.setting;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.setting.QAdmin;
import com.flyemu.share.entity.setting.QSystemLog;
import com.flyemu.share.entity.setting.SystemLog;
import com.flyemu.share.repository.SystemLogRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @功能描述: 操作日志
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SystemLogService extends AbsService {

    private final static QSystemLog qSystemLog = QSystemLog.systemLog;
    private final static QAdmin qAdmin = QAdmin.admin;

    private final SystemLogRepository systemLogRepository;

    public PageResults<Dict> query(Page page, Query query) {
        PagedList<Tuple> fetchPage = bqf.selectFrom(qSystemLog)
                .select(qSystemLog, qAdmin.name)
                .leftJoin(qAdmin).on(qAdmin.id.eq(qSystemLog.createdBy))
                .where(query.builder)
                .orderBy(qSystemLog.id.desc())
                .fetchPage(page.getOffset(), page.getOffsetEnd());

        List<Dict> rows = fetchPage.stream().collect(ArrayList::new, (list, tuple) -> {
            SystemLog item = tuple.get(qSystemLog);
            Dict row = Dict.create()
                    .set("id", item.getId())
                    .set("module", item.getModule())
                    .set("objectId", item.getObjectId())
                    .set("ipAddress", item.getIpAddress())
                    .set("requestParams", item.getRequestParams())
                    .set("operationType", item.getOperationType())
                    .set("createdBy", item.getCreatedBy())
                    .set("createdByName", tuple.get(qAdmin.name))
                    .set("createdAt", item.getCreatedAt())
                    .set("description", item.getDescription())
                    .set("accountBookId", item.getAccountBookId())
                    .set("merchantId", item.getMerchantId());
            list.add(row);
        }, List::addAll);
        return new PageResults<>(rows, page, fetchPage.getTotalSize());
    }

    @Transactional
    public SystemLog save(SystemLog systemLog) {
        if (systemLog.getCreatedAt() == null) {
            systemLog.setCreatedAt(LocalDateTime.now());
        }
        return systemLogRepository.save(systemLog);
    }

    /**
     * 记录操作日志
     */
    @Transactional
    public void record(String module,
                       SystemLog.OperationType operationType,
                       String description,
                       String ipAddress,
                       String requestParams,
                       Long objectId,
                       Long createdBy,
                       Long merchantId,
                       Long accountBookId) {
        if (merchantId == null || accountBookId == null || operationType == null) {
            return;
        }
        SystemLog systemLog = new SystemLog();
        systemLog.setModule(StrUtil.blankToDefault(module, "系统"));
        systemLog.setOperationType(operationType);
        systemLog.setDescription(description);
        systemLog.setIpAddress(ipAddress);
        if (StrUtil.isNotBlank(requestParams) && requestParams.length() > 2000) {
            systemLog.setRequestParams(requestParams.substring(0, 2000));
        } else {
            systemLog.setRequestParams(requestParams);
        }
        systemLog.setObjectId(objectId);
        systemLog.setCreatedBy(createdBy);
        systemLog.setCreatedAt(LocalDateTime.now());
        systemLog.setMerchantId(merchantId);
        systemLog.setAccountBookId(accountBookId);
        systemLogRepository.save(systemLog);
    }

    @Transactional
    public void delete(Long systemLogId, Long merchantId, Long accountBookId) {
        jqf.delete(qSystemLog)
                .where(qSystemLog.id.eq(systemLogId)
                        .and(qSystemLog.merchantId.eq(merchantId))
                        .and(qSystemLog.accountBookId.eq(accountBookId)))
                .execute();
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

        public void setOperationType(String operationType) {
            if (StrUtil.isNotBlank(operationType)) {
                builder.and(qSystemLog.operationType.eq(SystemLog.OperationType.valueOf(operationType)));
            }
        }

        public void setKeyword(String keyword) {
            if (StrUtil.isNotBlank(keyword)) {
                String value = keyword.trim();
                builder.and(qSystemLog.module.contains(value)
                        .or(qSystemLog.description.contains(value))
                        .or(qSystemLog.ipAddress.contains(value)));
            }
        }

        public void setStartTime(String startTime) {
            if (StrUtil.isNotBlank(startTime)) {
                builder.and(qSystemLog.createdAt.goe(LocalDateTime.parse(startTime + "T00:00:00")));
            }
        }

        public void setEndTime(String endTime) {
            if (StrUtil.isNotBlank(endTime)) {
                builder.and(qSystemLog.createdAt.loe(LocalDateTime.parse(endTime + "T23:59:59")));
            }
        }
    }
}
