package com.flyemu.share.hibernate;

import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.querydsl.BlazeJPAQuery;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;

@Component
@RequiredArgsConstructor
public class BlazeJPAQueryFactory {

    protected final EntityManager em;

    protected final CriteriaBuilderFactory cbf;

    public <T> BlazeJPAQuery<T> selectFrom(EntityPath<T> from) {
        BlazeJPAQuery<T> query = new BlazeJPAQuery<T>(em, cbf);
        return query.select(from).from(from);
    }
    public <T> BlazeJPAQuery<T> select(Expression<T> expr) {
        return new BlazeJPAQuery<T>(em, cbf).select(expr);
    }
    public BlazeJPAQuery<Tuple> select(Expression<?>... exprs) {
        return new BlazeJPAQuery<Tuple>(em, cbf).select(exprs);
    }

}
