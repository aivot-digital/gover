package de.aivot.GoverBackend.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.Optional;

@NoRepositoryBean
public interface ReadOnlyRepository <T, ID> extends Repository<T, ID> {
    Collection<T> findAll();

    Collection<T> findAll(Sort sort);

    Page<T> findAll(Pageable pageable);

    Optional<T> findById(ID id);

    long count();
}
