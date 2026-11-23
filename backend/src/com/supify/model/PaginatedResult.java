package com.supify.model;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Generic page slice wrapper supporting type-safe functional mapping. */
public class PaginatedResult<T> {
    private final List<T> items;
    private final int totalCount;
    private final int page;
    private final int pageSize;

    public PaginatedResult(List<T> items, int totalCount, int page, int pageSize) {
        this.items = items != null ? Collections.unmodifiableList(items) : Collections.emptyList();
        this.totalCount = totalCount;
        this.page = page;
        this.pageSize = pageSize;
    }

    public List<T> getItems() {
        return items;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalPages() {
        return (int) Math.ceil((double) totalCount / pageSize);
    }

    public <R> PaginatedResult<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mappedItems = items.stream()
                .map(mapper)
                .collect(Collectors.toList());
        return new PaginatedResult<>(mappedItems, totalCount, page, pageSize);
    }
}
