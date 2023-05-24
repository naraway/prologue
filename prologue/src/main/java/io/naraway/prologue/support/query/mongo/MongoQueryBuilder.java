package io.naraway.prologue.support.query.mongo;

import io.naraway.accent.domain.message.dynamic.Connector;
import io.naraway.accent.domain.message.dynamic.QueryParams;
import io.naraway.accent.domain.type.Offset;
import io.naraway.accent.util.json.JsonUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MongoQueryBuilder {
    //
    public static Query build(MongoQueryRequest request) {
        //
        AtomicReference<Criteria> criteria = new AtomicReference<>();
        criteria.set(buildCriteria(request.getQueryParams()));

        return new Query(criteria.get());
    }

    public static Query build(MongoQueryRequest request, Offset offset) {
        //
        Query query = build(request);

        if (offset == null) {
            return query;
        }

        if (offset.getOffset() != -1 && offset.getLimit() != -1) {
            query = fetchOffset(query, offset);
        }
        if (StringUtils.hasText(offset.getSortingField()) && offset.getSortDirection() != null) {
            query = fetchSort(query, offset);
        }

        return query;
    }

    private static Criteria buildCriteria(QueryParams queryParams) {
        //
        if (Optional.ofNullable(queryParams).isEmpty() || queryParams.isEmpty()) {
            return Criteria.where("id").ne("*");
        }

        return queryParams.stream().map(queryParam -> {
                    String name = queryParam.getName();
                    Connector connector = queryParam.getConnector();
                    String value = queryParam.getValue();

                    switch (queryParam.getOperator()) {
                        case NotEqual:
                            return Map.entry(new Criteria().orOperator(parseValue(value).stream()
                                    .map(v -> Criteria.where(name).ne(v))
                                    .collect(Collectors.toList())), connector);
                        case In:
                            return Map.entry(new Criteria().orOperator(
                                    Criteria.where(name).in(JsonUtil.fromJsonList(value, Object.class)),
                                    Criteria.where(name).in(JsonUtil.fromJsonList(value, String.class))), connector);
                        case NotIn:
                            return Map.entry(new Criteria().orOperator(
                                    Criteria.where(name).nin(JsonUtil.fromJsonList(value, Object.class)),
                                    Criteria.where(name).nin(JsonUtil.fromJsonList(value, String.class))), connector);
                        case Like:
                        case Regex:
                            return Map.entry(Criteria.where(name).regex(value, "i"), connector);
                        case LessThan:
                            return Map.entry(new Criteria().orOperator(parseValue(value).stream()
                                    .map(v -> Criteria.where(name).lt(v))
                                    .collect(Collectors.toList())), connector);
                        case LessThanOrEqual:
                            return Map.entry(new Criteria().orOperator(parseValue(value).stream()
                                    .map(v -> Criteria.where(name).lte(v))
                                    .collect(Collectors.toList())), connector);
                        case GreaterThan:
                            return Map.entry(new Criteria().orOperator(parseValue(value).stream()
                                    .map(v -> Criteria.where(name).gt(v))
                                    .collect(Collectors.toList())), connector);
                        case GreaterThanOrEqual:
                            return Map.entry(new Criteria().orOperator(parseValue(value).stream()
                                    .map(v -> Criteria.where(name).gte(v))
                                    .collect(Collectors.toList())), connector);
                        case Equal:
                        default:
                            return Map.entry(new Criteria().orOperator(parseValue(value).stream()
                                    .map(v -> Criteria.where(name).is(v))
                                    .collect(Collectors.toList())), connector);
                    }
                })
                .reduce((prevCriteriaEntry, nextCriteriaEntry) -> {
                    Criteria prev = prevCriteriaEntry.getKey();
                    Criteria next = nextCriteriaEntry.getKey();
                    // And & Or Operator's priority is only depends on order.
                    switch (prevCriteriaEntry.getValue()) {
                        case Or:
                            return Map.entry(new Criteria().orOperator(prev, next), nextCriteriaEntry.getValue());
                        case And:
                        default:
                            return Map.entry(new Criteria().andOperator(prev, next), nextCriteriaEntry.getValue());
                    }
                })
                .map(Map.Entry::getKey)
                .orElseGet(() -> Criteria.where("id").is("*"));
    }

    private static Query fetchOffset(Query query, Offset offset) {
        //
        if (nonNull(offset) && offset.getOffset() >= 0 && offset.getLimit() != 0) {
            return query.with(PageRequest.of(offset.page(), offset.getLimit()));
        }

        return query;
    }

    private static Query fetchSort(Query query, Offset offset) {
        //
        if (nonNull(offset) && nonNull(offset.getSortingField()) && !offset.getSortingField().isEmpty()) {
            Sort.Direction direction = offset.ascendingSort() ? Sort.Direction.ASC : Sort.Direction.DESC;
            return query.with(Sort.by(direction, offset.getSortingField()));
        }

        return query;
    }

    private static List<Object> parseValue(String value) {
        //
        if (value == null || value.equals("null")) {
            return Collections.singletonList(null);
        } else if (Arrays.asList("true", "false").contains(value)) {
            return List.of(Boolean.parseBoolean(value));
        } else if (isObject(value)) {
            return Collections.singletonList(JsonUtil.fromJson(value, Object.class));
        } else {
            List<Object> objects = new ArrayList<>();
            objects.add(value);

            if (isNumeric(value)) {
                objects.add(JsonUtil.fromJson(value, Object.class));
            }

            return objects;
        }
    }

    private static boolean isNumeric(String value) {
        //
        return value.matches("[+-]?\\d*(\\.\\d+)?");
    }

    private static boolean isObject(String value) {
        //
        return value.matches("\\{.*\\}");
    }
}