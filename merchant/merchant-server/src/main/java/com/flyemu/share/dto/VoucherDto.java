package com.flyemu.share.dto;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class VoucherDto implements Serializable {
    private Long id;
    private String word;
    private Integer code;
    private String remark;
    private Integer receiptNum;
    private Long createMember;
    private String createMemberName;
    private String createMemberMobile;
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createDate;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private Long accountSetsId;
    private Integer year;
    private Integer month;
    private LocalDate voucherDate;
    private Long auditMemberId;
    private Long invoiceId;
    private List<Long> invoiceIds;
    private Long journalId;
    private List<Long> journalIds;
    private Long cashierTransferId;
    private List<Long> cashierTransferIds;
    private String auditMemberName;
    private LocalDateTime auditDate;
    private Boolean carryForward;
    private List<VoucherDetailsDto> details = new ArrayList<>(0);
    private LocalDate yearMonth;
    private Boolean isTemplate;
    private String templateName;
    private Boolean locked;
    private Boolean insert = false;
    private Long checkTplId;
    private Long reverseVoucherId;

    public List<Long> getCashierTransferIds() {
        if (CollUtil.isNotEmpty(cashierTransferIds)) {
            return cashierTransferIds;
        }
        return cashierTransferId != null ? CollUtil.newArrayList(cashierTransferId) : null;
    }

    public List<Long> getJournalIds() {
        if (CollUtil.isNotEmpty(journalIds)) {
            return journalIds;
        }
        return journalId != null ? CollUtil.newArrayList(journalId) : null;
    }
}
