package io.github.hexfaker.springdataextensions;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

/**
 * Controls what is fetched in JPA criteria API queries done with
 * {@link org.springframework.data.jpa.domain.Specification}
 *
 * @author Vsevolod Poletaev
 */
public interface SelectionSpecification<T, R> {
  /**
   * Creates a SELECT clause for a query of the referenced entity in form of a {@link Selection} for the given
   * {@link Root} and {@link CriteriaQuery}.
   *
   * @param root Entity reference
   * @param query CriteriaQuery
   * @return a {@link Selection}, must not be {@literal null}.
   */
  Selection<R> toSelection(Root<T> root, CriteriaQuery<? extends R> query, CriteriaBuilder cb);

  Class<R> getResultClass();
}
