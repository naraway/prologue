package io.naraway.prologue.support.query.mongo;

import io.naraway.accent.domain.message.dynamic.DynamicQuery;
import io.naraway.accent.domain.message.dynamic.QueryParams;
import io.naraway.accent.domain.type.Offset;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

public class MongoDynamicQuery<T> implements DynamicQuery<T> {
    //
    private final Class<T> typeClass;
    private final QueryParams queryParams;
    private final Offset offset;
    private final MongoQueryRequest<T> request;

    public MongoDynamicQuery(MongoTemplate mongoTemplate, QueryParams queryParams, Class<T> typeClass) {
        //
        this(mongoTemplate, queryParams, Offset.newDefault(), typeClass);
    }

    public MongoDynamicQuery(MongoTemplate mongoTemplate, QueryParams queryParams, Offset offset, Class<T> typeClass) {
        //
        this.request = new MongoQueryRequest<>(mongoTemplate);
        this.queryParams = queryParams;
        this.offset = offset;
        this.typeClass = typeClass;
    }

    @Override
    public T findOne() {
        //
        this.request.addQueryParamsAndClass(this.queryParams, this.typeClass);
        Query query = MongoQueryBuilder.build(this.request);

        return request.findOne(query);
    }

    @Override
    public List<T> findAll() {
        //
        this.request.addQueryParamsAndClass(this.queryParams, this.typeClass);

        Query query = MongoQueryBuilder.build(this.request, this.offset);
        List<T> docs = this.request.findAll(query);

        Query countQuery = MongoQueryBuilder.build(this.request);
        long count = this.request.count(countQuery);
        updateOffset(count);

        return docs;
    }

    private void updateOffset(long count) {
        //
        this.offset.setTotalCount(count);
        this.offset.setPrevious(this.offset.getOffset() > 0);
        this.offset.setNext(count > this.offset.getLimit());
    }
}