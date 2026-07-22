package com.flyemu.share.form;

import com.flyemu.share.entity.fund.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class VerificationForm {

    private Verification order;
    private List<VerificationCollection> collectionList;
    private List<VerificationItem> itemList;
}
