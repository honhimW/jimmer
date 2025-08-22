package org.babyfish.jimmer;

import org.babyfish.jimmer.model.Author;
import org.babyfish.jimmer.model.AuthorDraft;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.validation.ValidationException;

public class AuthorTest {

    @Test
    public void testValidEmail() {
        Assertions.assertEquals(
                "{\"email\":\"tom@gmail.com\"}",
                AuthorDraft.$.produce(a -> a.setEmail("tom@gmail.com")).toString()
        );
    }

    @Test
    public void testInvalidEmail() {
        Assertions.assertThrows(ValidationException.class, () -> {
            AuthorDraft.$.produce(a -> a.setEmail("tomgmail.com"));
        });
        Assertions.assertThrows(ValidationException.class, () -> {
            AuthorDraft.$.produce(a -> a.setEmail("tom@@gmail.com"));
        });
        Assertions.assertThrows(ValidationException.class, () -> {
            AuthorDraft.$.produce(a -> a.setEmail("@tomgmail.com"));
        });
        Assertions.assertThrows(ValidationException.class, () -> {
            AuthorDraft.$.produce(a -> a.setEmail("tomgmail.com@"));
        });
    }

    @Test
    public void testTrait() {
        Author foo = AuthorDraft.$.produce(a -> a.setName("foo"));
        String apply = foo.doFuck("bar").apply("call");
        assert "call foo fuck bar".equals(apply);
    }
}
