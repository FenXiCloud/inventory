package com.flyemu.share.form;

import com.flyemu.share.entity.basic.ProductCombo;
import com.flyemu.share.entity.basic.ProductComboItem;
import lombok.Data;

import java.util.List;

@Data
public class ProductComboForm {

    private ProductCombo combo;

    private List<ProductComboItem> comboItemList;
}
