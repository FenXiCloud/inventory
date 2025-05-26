package com.flyemu.share.service.fund.dto;

import com.flyemu.share.entity.fund.*;
import com.flyemu.share.enums.OrderStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author shuaiqi
 */
@Data
@NoArgsConstructor
public class OrderPaymentSaveDTO {

    private OrderPayment orderPayment;
    private List<OrderPaymentCollection> collectionList;
    private List<OrderPaymentItem> itemList;

}
