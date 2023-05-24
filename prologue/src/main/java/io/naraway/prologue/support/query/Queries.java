package io.naraway.prologue.support.query;

import io.naraway.accent.domain.type.Offset;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.util.StringUtils;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Queries {
    //
    private static final String DEFAULT_SORT_FIELD = "id";

    public static Pageable pageable(Offset offset) {
        //
        if (offset == null) {
            return PageRequest.of(1, Integer.MAX_VALUE, Sort.Direction.ASC, DEFAULT_SORT_FIELD);
        }

        if (offset.getSortDirection() != null && offset.getSortingField() != null) {
            return PageRequest.of(
                    offset.page(),
                    offset.limit(),
                    (offset.ascendingSort() ? Sort.Direction.ASC : Sort.Direction.DESC),
                    (StringUtils.hasText(offset.getSortingField()) ? offset.getSortingField() : DEFAULT_SORT_FIELD)
            );
        } else {
            return PageRequest.of(offset.page(), offset.limit());
        }
    }

    public static <T> Page<T> page(List<T> list, Offset offset) {
        //
        return new PageImpl<>(list, pageable(offset), offset.getTotalCount());
    }

    public static <T> Slice<T> slice(List<T> list, Offset offset) {
        //
        return new SliceImpl<>(list, pageable(offset), offset.hasNext());
    }
}
