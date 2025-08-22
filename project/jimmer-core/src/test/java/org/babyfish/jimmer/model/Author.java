package org.babyfish.jimmer.model;

import org.babyfish.jimmer.Formula;
import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.sql.Trait;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.function.Function;

@Immutable
public interface Author {

    @NotBlank
    @Size(min = 1, max = 50)
    String name();

    List<Book> books();

    @Email
    String email();

    @Trait
    default Function<String, String> doFuck(String other) {
        return s -> s + " " + this.name() + " fuck " + other;
    }

}
