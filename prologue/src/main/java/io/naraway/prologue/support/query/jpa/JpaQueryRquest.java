package io.naraway.prologue.support.query.jpa;

import io.naraway.accent.domain.message.dynamic.QueryParams;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.persistence.EntityManager;

@Getter
@RequiredArgsConstructor
public class JpaQueryRquest<T> {
    //
    private final EntityManager entityManager;

    private QueryParams queryParams;
    private Class<T> typeClass;

    public void addQueryParamsAndClass(QueryParams queryParams, Class<T> typeClass) {
        //
        this.queryParams = queryParams;
        this.typeClass = typeClass;
    }
}