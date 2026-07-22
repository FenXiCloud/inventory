package com.flyemu.share.repository.setting;

import com.flyemu.share.entity.setting.FinanceAccountLink;
import com.flyemu.share.entity.setting.FinanceVoucherTemplate;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;

import java.util.List;

public interface FinanceVoucherTemplateRepository extends JpaRepositoryImplementation<FinanceVoucherTemplate, Long> {

    List<FinanceVoucherTemplate> findByType(String type);
}
