package com.flyemu.share.form;

import com.flyemu.share.entity.purchase.PurchaseReservation;
import com.flyemu.share.entity.purchase.PurchaseReservationItem;
import lombok.Data;

import java.util.List;

@Data
public class PurchaseReservationForm {

    private PurchaseReservation purchaseReservation;

    private List<PurchaseReservationItem> purchaseReservationItemList;

    private List<Long> selectPurchaseReservationIdList;
}
