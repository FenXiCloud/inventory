package com.flyemu.share.form;

import com.flyemu.share.entity.sales.SalesReservation;
import com.flyemu.share.entity.sales.SalesReservationItem;
import lombok.Data;

import java.util.List;

@Data
public class SalesReservationForm {

    private SalesReservation salesReservation;

    private List<SalesReservationItem> salesReservationItemList;

    private List<Long> selectSalesReservationIdList;
}
