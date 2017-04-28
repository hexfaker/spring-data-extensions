package io.github.hexfaker.springdataextensions;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * Interface to allow execution of {@link org.springframework.data.jpa.domain.Specification}s based on the JPA criteria
 * API with {@link SelectionSpecification} support.
 *
 * @author Vsevolod Poletaev
 */
public interface JpaSpecificationWithSelectionExecutor<T> {

  <R> R findOne(Specification<T> spec, SelectionSpecification<T, R> selection);

  <R> List<R> findAll(Specification<T> spec, SelectionSpecification<T, R> selection);

  <R> List<R> findAll(Specification<T> spec, SelectionSpecification<T, R> selection, Sort sort);

  <R> Page<R> findAll(Specification<T> spec, SelectionSpecification<T, R> selection, Pageable pageable);
}
