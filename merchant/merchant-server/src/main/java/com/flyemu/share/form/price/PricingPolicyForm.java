package com.flyemu.share.form.price;

import com.flyemu.share.dto.price.PricingPolicyDto;
import com.flyemu.share.entity.basic.PricingPolicy;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import java.io.Serializable;
import java.util.List;

@Data
public class PricingPolicyForm implements Serializable {

    private List<PricingPolicyDto> dataList;

}

