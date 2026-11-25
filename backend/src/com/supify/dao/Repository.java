package com.supify.dao;

import java.util.List;

// Generic CRUD interface shared by in-memory and MySQL storage
public interface Repository<T, ID> {
    T save(T item);

    T findById(ID id);

    List<T> findAll();

    boolean deleteById(ID id);

    long count();
}
