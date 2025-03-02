package com.flyemu.share.form.price;

import com.flyemu.share.dto.price.PricingPolicyDTO;
import com.flyemu.share.entity.basic.PricingPolicy;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import java.io.Serializable;
import java.util.List;

/**
 * @功能描述: 价格取数规则(销售采购价格取数优先级）
 * @创建时间: 2024年04月28日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Data
public class PricingPolicyForm implements Serializable {

    private List<PricingPolicyDTO> dataList;

}

