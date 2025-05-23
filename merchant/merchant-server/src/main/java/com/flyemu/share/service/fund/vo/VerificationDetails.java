package com.flyemu.share.service.fund.vo;

import com.flyemu.share.entity.fund.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 *@author shuaiqi
 */
@Data
@NoArgsConstructor
public class VerificationDetails {
    private VerificationVO order;
    private List<VerificationCollection> collectionList;
    private List<VerificationItem> itemList;
}
