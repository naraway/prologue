package io.naraway.prologue.support.query.jpa;

import io.naraway.accent.domain.message.dynamic.DynamicQuery;
import io.naraway.accent.domain.message.dynamic.QueryParams;
import io.naraway.accent.domain.type.Offset;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

public class JpaDynamicQuery<T> implements DynamicQuery<T> {
    //
    private final Class<T> typeClass;
    private final QueryParams queryParams;
    private final Offset offset;
    private final JpaQueryRquest<T> request;

    public JpaDynamicQuery(EntityManager entityManager, QueryParams queryParams, Class<T> typeClass) {
        //
        this(entityManager, queryParams, Offset.newDefault(), typeClass);
    }

    public JpaDynamicQuery(EntityManager entityManager, QueryParams queryParams, Offset offset, Class<T> typeClass) {
        //
        this.request = new JpaQueryRquest<>(entityManager);
        this.queryParams = queryParams;
        this.offset = offset;
        this.typeClass = typeClass;
    }

    @Override
    public T findOne() {
        //
        this.request.addQueryParamsAndClass(this.queryParams, this.typeClass);
        TypedQuery<T> query = JpaQueryBuilder.build(this.request);

        return query.getSingleResult();
    }

    @Override
    public List<T> findAll() {
        //
        this.request.addQueryParamsAndClass(this.queryParams, this.typeClass);

        TypedQuery<T> query = JpaQueryBuilder.build(this.request, offset);
        query.setFirstResult(this.offset.getOffset());
        query.setMaxResults(this.offset.getLimit());
        List<T> jpos = query.getResultList();

        TypedQuery<Long> countQuery = JpaQueryBuilder.buildForCount(this.request);
        long count = countQuery.getSingleResult();
        updateOffset(count);

        return jpos;
    }

    private void updateOffset(long count) {
        //
        this.offset.setTotalCount(count);
        this.offset.setPrevious(this.offset.getOffset() > 0);
        this.offset.setNext(count > this.offset.getLimit());
    }
}