package com.flyemu.share.service;

import com.flyemu.share.hibernate.BlazeJPAQueryFactory;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.sagacity.sqltoy.dao.SqlToyLazyDao;
import org.springframework.context.annotation.Lazy;

import jakarta.annotation.Resource;

/**
 * @功能描述: 共用Service
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
public abstract class AbsService {

    @Resource
    protected SqlToyLazyDao lazyDao;

    @Resource
    protected JPAQueryFactory jqf;

    @Resource
    @Lazy
    protected BlazeJPAQueryFactory bqf;

}
