package com.flyemu.share.repository;

import com.flyemu.share.entity.setting.FinanceAccountLink;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;

import java.util.List;


public interface FinanceAccountLinkRepository extends JpaRepositoryImplementation<FinanceAccountLink, Long> {

    List<FinanceAccountLink> findByAccountBookId(Long accountBookId);
}
