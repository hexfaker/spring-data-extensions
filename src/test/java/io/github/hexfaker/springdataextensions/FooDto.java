package io.github.hexfaker.springdataextensions;

/**
 * @author Vsevolod Poletaev (hexfaker)
 */
public class FooDto {
  private final String foo;

  public FooDto(String foo) {
    this.foo = foo;
  }

  public String getFoo() {return this.foo;}
}
