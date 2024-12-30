package com.flyemu.share.dto;

import com.flyemu.share.entity.basic.Supplier;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)

public class SupplierDto extends Supplier {
    private String categoryName;

}
