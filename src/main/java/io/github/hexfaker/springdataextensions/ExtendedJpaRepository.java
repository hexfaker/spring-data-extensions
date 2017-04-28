package io.github.hexfaker.springdataextensions;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.query.Jpa21Utils;
import org.springframework.data.jpa.repository.query.JpaEntityGraph;
import org.springframework.data.jpa.repository.support.CrudMethodMetadata;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import javax.persistence.*;
import javax.persistence.criteria.*;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.data.jpa.repository.query.QueryUtils.toOrders;

/**
 * Implements jpa extensions. Most methods are just copies of corresponding private ones from SimpleJpaRepository.
 *
 * @author Vsevolod Poletaev
 */
public class ExtendedJpaRepository<T, ID extends Serializable> extends SimpleJpaRepository<T, ID> implements JpaSpecificationWithSelectionExecutor<T> {

  private CrudMethodMetadata metadata;
  private final EntityManager em;
  private final JpaEntityInformation<T, ?> entityInformation;

  public ExtendedJpaRepository(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
    super(entityInformation, entityManager);
    em = entityManager;
    this.entityInformation = entityInformation;
  }

  /**
   * Executes a count query and transparently sums up all values returned.
   *
   * @param query must not be {@literal null}.
   */
  protected static Long executeCountQuery(TypedQuery<Long> query) {


    List<Long> totals = query.getResultList();
    Long total = 0L;

    for (Long element : totals) {
      total += element == null ? 0 : element;
    }

    return total;
  }

  /**
   * Creates a new {@link TypedQuery} from the given {@link Specification}.
   *
   * @param spec     can be {@literal null}.
   * @param pageable can be {@literal null}.
   */
  protected <R> TypedQuery<R> getQuery(Specification<T> spec, SelectionSpecification<T, R> selection,
                                       Pageable pageable) {

    Sort sort = pageable == null ? null : pageable.getSort();
    return getQuery(spec, getDomainClass(), selection, sort);
  }

  /**
   * Creates a new {@link TypedQuery} from the given {@link Specification}.
   *
   * @param spec        can be {@literal null}.
   * @param domainClass must not be {@literal null}.
   * @param pageable    can be {@literal null}.
   */
  protected <D extends T, R> TypedQuery<R> getQuery(Specification<D> spec, Class<D> domainClass,
                                                    SelectionSpecification<D, R> selection, Pageable pageable) {

    Sort sort = pageable == null ? null : pageable.getSort();
    return getQuery(spec, domainClass, selection, sort);
  }

  /**
   * Creates a {@link TypedQuery} for the given {@link Specification} and {@link Sort}.
   *
   * @param spec can be {@literal null}.
   * @param sort can be {@literal null}.
   */
  protected <R> TypedQuery<R> getQuery(Specification<T> spec, SelectionSpecification<T, R> selection, Sort sort) {
    return getQuery(spec, getDomainClass(), selection, sort);
  }

  /**
   * Creates a {@link TypedQuery} for the given {@link Specification} and {@link Sort}.
   *
   * @param spec        can be {@literal null}.
   * @param domainClass must not be {@literal null}.
   * @param sort        can be {@literal null}.
   */
  protected <D extends T, R> TypedQuery<R> getQuery(Specification<D> spec, Class<D> domainClass,
                                                    SelectionSpecification<D, R> selection, Sort sort) {

    CriteriaBuilder builder = em.getCriteriaBuilder();
    CriteriaQuery<R> query = builder.createQuery(selection.getResultClass());

    Root<D> root = applySpecificationToCriteria(spec, domainClass, query);
    query.select(selection.toSelection(root, query, builder));

    if (sort != null) {
      query.orderBy(toOrders(sort, root, builder));
    }

    return applyRepositoryMethodMetadata(em.createQuery(query));
  }

  /**
   * Reads the given {@link TypedQuery} into a {@link Page} applying the given {@link Pageable} and
   * {@link Specification}, allows result type differ from root entity type
   * (unlike {@link SimpleJpaRepository#readPage}).
   *
   * @param query    must not be {@literal null}.
   * @param spec     can be {@literal null}.
   * @param pageable can be {@literal null}.
   * @param <R> Result type
   */
  protected <R> Page<R> readPageNew(TypedQuery<R> query, Pageable pageable, Specification<T> spec) {
    return readPageNew(query, getDomainClass(), pageable, spec);
  }

  /**
   * Reads the given {@link TypedQuery} into a {@link Page} applying the given {@link Pageable} and
   * {@link Specification}, allows result type differ from root entity type
   * (unlike {@link SimpleJpaRepository#readPage).
   *
   * @param query       must not be {@literal null}.
   * @param domainClass must not be {@literal null}.
   * @param spec        can be {@literal null}.
   * @param pageable    can be {@literal null}.
   * @param <R> Result type
   * @param <D> Root entity type
   */
  protected <D extends T, R> Page<R> readPageNew(TypedQuery<R> query, Class<D> domainClass, Pageable pageable,
                                                 Specification<D> spec) {

    query.setFirstResult(pageable.getOffset());
    query.setMaxResults(pageable.getPageSize());

    Long total = executeCountQuery(getCountQuery(spec, domainClass));
    List<R> content = total > pageable.getOffset() ? query.getResultList() : Collections.emptyList();

    return new PageImpl<>(content, pageable, total);
  }


  @Override
  public <R> R findOne(Specification<T> spec, SelectionSpecification<T, R> selection) {
    try {
      return getQuery(spec, selection, (Sort) null).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  @Override
  public <R> List<R> findAll(Specification<T> spec, SelectionSpecification<T, R> selection) {
    return getQuery(spec, selection, (Sort) null).getResultList();
  }

  @Override
  public <R> List<R> findAll(Specification<T> spec, SelectionSpecification<T, R> selection, Sort sort) {
    return getQuery(spec, selection, sort).getResultList();
  }

  @Override
  public <R> Page<R> findAll(Specification<T> spec, SelectionSpecification<T, R> selection, Pageable pageable) {
    TypedQuery<R> query = getQuery(spec, selection, pageable);
    return pageable == null ? new PageImpl<>(query.getResultList()) : readPageNew(query, pageable, spec);
  }

  /**
   * Configures a custom {@link CrudMethodMetadata} to be used to detect {@link LockModeType}s and query hints to be
   * applied to queries.
   */
  public void setRepositoryMethodMetadata(CrudMethodMetadata crudMethodMetadata) {
    this.metadata = crudMethodMetadata;
  }

  protected Class<T> getDomainClass() {
    return entityInformation.getJavaType();
  }

  /**
   * Returns a {@link Map} with the query hints based on the current {@link CrudMethodMetadata} and potential
   * {@link EntityGraph} information.
   *
   */
  protected Map<String, Object> getQueryHints() {

    if (metadata.getEntityGraph() == null) {
      return metadata.getQueryHints();
    }

    Map<String, Object> hints = new HashMap<>();
    hints.putAll(metadata.getQueryHints());

    hints.putAll(Jpa21Utils.tryGetFetchGraphHints(em, getEntityGraph(), getDomainClass()));

    return hints;
  }

  private JpaEntityGraph getEntityGraph() {
    String fallbackName = this.entityInformation.getEntityName() + "." + metadata.getMethod().getName();
    return new JpaEntityGraph(metadata.getEntityGraph(), fallbackName);
  }

  protected <S> TypedQuery<S> applyRepositoryMethodMetadata(TypedQuery<S> query) {

    if (metadata == null) {
      return query;
    }

    LockModeType type = metadata.getLockModeType();
    TypedQuery<S> toReturn = type == null ? query : query.setLockMode(type);

    applyQueryHints(toReturn);

    return toReturn;
  }

  protected void applyQueryHints(Query query) {

    for (Map.Entry<String, Object> hint : getQueryHints().entrySet()) {
      query.setHint(hint.getKey(), hint.getValue());
    }
  }

  /**
   * Applies the given {@link Specification} to the given {@link CriteriaQuery}.
   *
   * @param spec can be {@literal null}.
   * @param domainClass must not be {@literal null}.
   * @param query must not be {@literal null}.
   * @param <R> Query result type
   * @param <D> Query root type
   */
  protected <R, D extends T> Root<D> applySpecificationToCriteria(Specification<D> spec, Class<D> domainClass,
                                                                  CriteriaQuery<R> query) {

    Root<D> root = query.from(domainClass);

    if (spec == null) {
      return root;
    }

    CriteriaBuilder builder = em.getCriteriaBuilder();
    Predicate predicate = spec.toPredicate(root, query, builder);

    if (predicate != null) {
      query.where(predicate);
    }

    return root;
  }

  /**
   * Creates a new count query for the given {@link Specification}.
   *
   * @param spec        can be {@literal null}.
   * @param domainClass must not be {@literal null}.
   * @param <D> Query root type
   */
  protected <D extends T> TypedQuery<Long> getCountQuery(Specification<D> spec, Class<D> domainClass) {

    CriteriaBuilder builder = em.getCriteriaBuilder();
    CriteriaQuery<Long> query = builder.createQuery(Long.class);

    Root<D> root = applySpecificationToCriteria(spec, domainClass, query);

    if (query.isDistinct()) {
      query.select(builder.countDistinct(root));
    } else {
      query.select(builder.count(root));
    }

    // Remove all Orders the Specifications might have applied
    query.orderBy(Collections.emptyList());

    return em.createQuery(query);
  }
}
