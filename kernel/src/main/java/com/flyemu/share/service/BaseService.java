package com.flyemu.share.service;

import com.flyemu.share.hibernate.BlazeJPAQueryFactory;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.sagacity.sqltoy.dao.SqlToyLazyDao;
import org.springframework.context.annotation.Lazy;

import jakarta.annotation.Resource;

public abstract class BaseService {

    @Resource
    protected SqlToyLazyDao lazyDao;

    @Resource
    protected JPAQueryFactory jqf;

    @Resource
    @Lazy
    protected BlazeJPAQueryFactory bqf;

}
