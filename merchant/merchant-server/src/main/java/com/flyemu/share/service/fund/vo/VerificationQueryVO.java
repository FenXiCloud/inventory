package com.flyemu.share.service.fund.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.flyemu.share.entity.fund.Verification;
import com.flyemu.share.entity.fund.VerificationCollection;
import com.flyemu.share.entity.fund.VerificationItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 *@author shuaiqi
 */@JsonInclude()
@EqualsAndHashCode(callSuper = true)
@Data
public class VerificationQueryVO extends Verification {
    private List<VerificationCollection> collectionList;
    private List<VerificationItem> itemList;
}
