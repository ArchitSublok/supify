package com.supify.dao;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Generic repository contract demonstrating generic interfaces, methods, and wildcards. */
public interface Repository<T, ID> {
    T save(T item);

    Optional<T> findById(ID id);

    List<T> findAll();

    List<T> findMatching(Predicate<? super T> filter);

    boolean deleteById(ID id);

    long count();

    static <T, R> List<R> mapList(List<T> items, Function<? super T, ? extends R> mapper) {
        return items.stream().map(mapper).collect(Collectors.toList());
    }
}
