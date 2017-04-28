package io.github.hexfaker.springdataextensions;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Vsevolod Poletaev (hexfaker)
 */
@Entity
public class FooBar {
  private String foo;
  @Id
  private Long bar;

  public FooBar(String foo, Long bar) {
    this.foo = foo;
    this.bar = bar;
  }

  public FooBar() {}
}

