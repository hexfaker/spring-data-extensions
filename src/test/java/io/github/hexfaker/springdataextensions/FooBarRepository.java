package io.github.hexfaker.springdataextensions;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Vsevolod Poletaev (hexfaker)
 */
public interface FooBarRepository extends JpaRepository<FooBar, Long>, JpaSpecificationWithSelectionExecutor<FooBar> {
}
