package com.flyemu.share.form;

import com.alibaba.fastjson.JSONObject;
import com.flyemu.share.entity.basic.Product;
import lombok.Data;

import java.util.List;

@Data
public class ProductForm {

    private Product product;

    private List<JSONObject> levelPriceList;
}
