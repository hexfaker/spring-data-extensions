package io.github.hexfaker.springdataextensions;

import org.assertj.core.api.Condition;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * @author Vsevolod Poletaev (hexfaker)
 */
@RunWith(SpringRunner.class)
@DataJpaTest
public class SelectionSpecificationTest {

  @Configuration
  @EnableJpaRepositories(repositoryBaseClass = ExtendedJpaRepository.class)
  public static class Config {}

  private static final String
    FIRST_FOO = "foo",
    SECOND_FOO = "bar";

  @Autowired
  private FooBarRepository repo;

  @Test
  public void shouldRetreiveOneWithSelection() throws Exception {
    repo.save(asList(new FooBar(FIRST_FOO, 1L), new FooBar(SECOND_FOO, 2L)));

    FooDto dto = repo.findOne(barEquals(1), new FooDtoSelection());

    assertThat(dto).isNotNull();
    assertThat(dto.getFoo()).isEqualTo(FIRST_FOO);

  }

  @Test
  public void shouldRetreiveAllWithSelection() throws Exception {
    repo.save(asList(new FooBar(FIRST_FOO, 1L), new FooBar(SECOND_FOO, 2L), new FooBar(SECOND_FOO, 3L)));

    final List<FooDto> all = repo.findAll(fooEquals(SECOND_FOO), new FooDtoSelection());

    assertThat(all)
      .isNotNull()
      .hasSize(2)
      .are(fooEquals());
  }

  @Test
  public void shouldRetreivePageWithSelection() throws Exception {
    repo.save(asList(new FooBar(FIRST_FOO, 1L), new FooBar(SECOND_FOO, 2L), new FooBar(SECOND_FOO, 3L)));

    Page<FooDto> all = repo.findAll(barEquals(1).or(barEquals(2)).or(barEquals(3)),
      new FooDtoSelection(), new PageRequest(0, 1, new Sort(new Sort.Order(Sort.Direction.ASC, "foo"))));

    assertThat(all.getTotalElements()).isEqualTo(3);
    assertThat(all.getSize()).isEqualTo(1);
    assertThat(all.getContent().get(0).getFoo()).isEqualTo(SECOND_FOO);  // bar is earlier in alphabetical order
  }



  private static Condition<FooDto> fooEquals() {
    return new Condition<>(foo -> foo.getFoo().equals(SECOND_FOO), "Foo equals SECOND_FOO");
  }

  private static class FooDtoSelection implements SelectionSpecification<FooBar, FooDto> {

    @Override
    public Selection<FooDto> toSelection(Root<FooBar> root, CriteriaQuery<? extends FooDto> query, CriteriaBuilder cb) {
      return cb.construct(FooDto.class, root.get(FooBar_.foo));
    }

    @Override
    public Class<FooDto> getResultClass() {
      return FooDto.class;
    }
  }

  private static Specifications<FooBar> barEquals(long val) {
    return Specifications.where((root, query, cb) -> cb.equal(root.get(FooBar_.bar), val));
  }

  private static Specifications<FooBar> fooEquals(String val) {
    return Specifications.where((root, query, cb) -> cb.equal(root.get(FooBar_.foo), val));
  }
}
