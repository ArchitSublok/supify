package com.supify.dao;

import com.supify.model.Identifiable;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** Generic CRUD repository contract with wildcard predicate filtering. */
public interface GenericRepository<T extends Identifiable<ID>, ID> {
    T save(T entity);

    Optional<T> findById(ID id);

    List<T> findAll();

    List<T> findMatching(Predicate<? super T> predicate);

    boolean deleteById(ID id);

    long count();
}
