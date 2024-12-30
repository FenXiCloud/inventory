package com.flyemu.share.dto;

import com.flyemu.share.entity.Supplier;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)

public class SupplierDto extends Supplier {
    private String categoryName;

}
