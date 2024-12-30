package com.flyemu.share.hibernate;

import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.querydsl.BlazeJPAQuery;
import com.querydsl.core.types.EntityPath;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;

/**
 * @功能描述: BlazeJPAQueryFactory
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Component
@RequiredArgsConstructor
public class BlazeJPAQueryFactory {

    protected final EntityManager em;

    protected final CriteriaBuilderFactory cbf;

    public <T> BlazeJPAQuery<T> selectFrom(EntityPath<T> from) {
        BlazeJPAQuery<T> query = new BlazeJPAQuery<T>(em, cbf);
        return query.select(from).from(from);
    }
}
