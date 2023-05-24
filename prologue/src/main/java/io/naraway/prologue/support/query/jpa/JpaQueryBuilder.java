package io.naraway.prologue.support.query.jpa;

import com.github.tennaito.rsql.jpa.JpaPredicateVisitor;
import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import io.naraway.accent.domain.message.dynamic.Connector;
import io.naraway.accent.domain.message.dynamic.Operator;
import io.naraway.accent.domain.message.dynamic.QueryParams;
import io.naraway.accent.domain.type.Offset;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JpaQueryBuilder {
    //
    private static final List<Operator> specialOperator = Arrays.asList(Operator.Like, Operator.In, Operator.NotIn);

    @SuppressWarnings("java:S3740")
    public static <T> TypedQuery<T> build(JpaQueryRquest request) {
        //
        CriteriaQuery<T> query = buildQuery(request);
        return request.getEntityManager().createQuery(query);
    }

    public static <T> TypedQuery<T> build(JpaQueryRquest<T> request, Offset offset) {
        //
        CriteriaQuery<T> criteriaQuery = buildQuery(request, offset);

        TypedQuery<T> query = request.getEntityManager().createQuery(criteriaQuery);

        if (offset.getOffset() != -1 && offset.getLimit() != -1) {
            query.setFirstResult(offset.getOffset());
            query.setMaxResults(offset.getLimit());
        }

        return query;
    }

    public static <T> TypedQuery<Long> buildForCount(JpaQueryRquest<T> request) {
        //
        CriteriaQuery<Long> countQuery = buildCountQuery(request);
        return request.getEntityManager().createQuery(countQuery);
    }

    private static <T> CriteriaQuery<T> buildQuery(JpaQueryRquest<T> request) {
        //
        CriteriaBuilder builder = request.getEntityManager().getCriteriaBuilder();
        CriteriaQuery<T> query = builder.createQuery(request.getTypeClass());
        processWhereClause(request, query);

        return query;
    }

    private static <T> CriteriaQuery<T> buildQuery(JpaQueryRquest<T> request, Offset offset) {
        //
        CriteriaBuilder builder = request.getEntityManager().getCriteriaBuilder();
        CriteriaQuery<T> query = builder.createQuery(request.getTypeClass());
        Root<T> root = processWhereClause(request, query);

        if (offset == null) {
            return query;
        }

        if (StringUtils.hasText(offset.getSortingField()) && offset.getSortDirection() != null) {
            if (offset.ascendingSort()) {
                query.orderBy(builder.asc(root.get(offset.getSortingField())));
            } else {
                query.orderBy(builder.desc(root.get(offset.getSortingField())));
            }
        }

        return query;
    }

    private static <T> CriteriaQuery<Long> buildCountQuery(JpaQueryRquest<T> request) {
        //
        CriteriaBuilder builder = request.getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<T> root = processWhereClause(request, query);

        query.select(builder.count(root));

        return query;
    }

    private static <T> Root<T> processWhereClause(JpaQueryRquest<T> request, CriteriaQuery<?> criteria) {
        //
        Root<T> root = criteria.from(request.getTypeClass());
        RSQLVisitor<Predicate, EntityManager> visitor = new JpaPredicateVisitor<>().defineRoot(root);

        Set<ComparisonOperator> operators = RSQLOperators.defaultOperators();
        operators.add(new ComparisonOperator("=like=", false));

        Node node = new RSQLParser(operators).parse(toQueryString(request.getQueryParams()));
        Predicate predicate = node.accept(visitor, request.getEntityManager());

        criteria.where(predicate);
        return root;
    }

    private static String toQueryString(QueryParams queryParams) {
        //
        StringBuilder query = new StringBuilder();

        queryParams.stream().forEach(queryParam -> {
            query.append(queryParam.getName());
            query.append(queryParam.getOperator().operatorString());
            if (specialOperator.contains(queryParam.getOperator())) {
                query.append(parseValue(queryParam.getOperator(), queryParam.getValue()));
            } else {
                query.append(queryParam.getValue());
            }
            query.append(queryParam.getConnector().connectorString());
        });

        String result = query.toString().trim();

        if (result.endsWith(Connector.And.connectorString()) || result.endsWith(Connector.Or.connectorString())) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }

    @SuppressWarnings("java:S5361")
    private static String parseValue(Operator operator, String value) {
        //
        String parsedQueryValues = "";
        String inQueryValues = value.trim();

        switch (operator) {
            case Like:
                String regex = ".*";
                inQueryValues = regex.concat(inQueryValues);
                parsedQueryValues = inQueryValues.concat(regex);
                break;
            case In:
            case NotIn:
                //["a","b"]
                inQueryValues = inQueryValues.replaceAll("\"", "");
                inQueryValues = inQueryValues.replace("[", "(");
                parsedQueryValues = inQueryValues.replace("]", ")");
                break;
            default:
                throw new IllegalArgumentException("Unknown operator: " + operator.operatorString());
        }

        return parsedQueryValues;
    }
}