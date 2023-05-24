package io.naraway.prologue.support.query.mongo;

import io.naraway.accent.domain.message.dynamic.QueryParams;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class MongoQueryRequest<T> {
    //
    private final MongoTemplate mongoTemplate;

    private QueryParams queryParams;
    private Class<T> typeClass;
    private String collectionName;

    public void addQueryParamsAndClass(QueryParams queryParams, Class<T> typeClass) {
        //
        this.queryParams = queryParams;
        this.typeClass = typeClass;
        this.collectionName = Arrays.stream(this.typeClass.getAnnotations())
                .filter(Document.class::isInstance).findFirst()
                .map(annotation -> ((Document) annotation).collection())
                .orElseThrow(() -> new IllegalStateException(String.format("@Doc not defined in %s", typeClass.getName())));
    }

    public T findOne(Query query) {
        //
        return this.mongoTemplate.findOne(query, getTypeClass(), this.collectionName);
    }

    public List<T> findAll(Query query) {
        //
        return this.mongoTemplate.find(query, getTypeClass(), this.collectionName);
    }

    public long count(Query query) {
        //
        return this.mongoTemplate.count(query, getTypeClass(), this.collectionName);
    }
}