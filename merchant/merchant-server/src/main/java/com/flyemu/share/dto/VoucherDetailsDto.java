package com.flyemu.share.dto;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class VoucherDetailsDto implements Serializable {
    private Long id;
    private Long accountSetsId;
    private Long voucherId;
    private Integer year;
    private Integer month;
    private LocalDate voucherDate;
    private Long currencyId;
    private String currencyName;
    private String currencyCode;
    private BigDecimal originalAmount = BigDecimal.ZERO;
    private String summary;
    private Long subjectId;
    private String subjectName;
    private String subjectCode;
    private BigDecimal debitAmount = BigDecimal.ZERO;
    private BigDecimal creditAmount = BigDecimal.ZERO;
    private BigDecimal num = BigDecimal.ZERO;
    private BigDecimal price = BigDecimal.ZERO;
    private Long auxiliaryDetailId;
    private BigDecimal debitAmountFor = BigDecimal.ZERO;
    private BigDecimal creditAmountFor = BigDecimal.ZERO;
    private BigDecimal exchangeRate = BigDecimal.ZERO;
    private BigDecimal debitNum = BigDecimal.ZERO;
    private BigDecimal creditNum = BigDecimal.ZERO;
    private Boolean carryForward;
    private Boolean cashFlow;
    private List<AcDetailsDto> auxiliary;

    /**
     * 获取科目名称
     *
     * @return
     */
    public String getSubjectName() {
        return this.subjectCode + " " + StrUtil.replace(subjectName, "-", "_");
    }

    /**
     * 获取摘要
     *
     * @return
     */
    public void setSummary(String summary) {
        this.summary = StrUtil.trimToEmpty(summary);
    }

    /**
     * 获取金额
     *
     * @return
     */
    public BigDecimal getAmount() {
        if (debitAmount != null && !NumberUtil.equals(debitAmount, BigDecimal.ZERO)) {
            return debitAmount;
        } else {
            return creditAmount;
        }
    }

    /**
     * 获取外币金额
     *
     * @return
     */
    public BigDecimal getAmountFor() {
        if (debitAmountFor != null && !NumberUtil.equals(debitAmountFor, BigDecimal.ZERO)) {
            return debitAmountFor;
        } else {
            return creditAmountFor;
        }
    }
}
