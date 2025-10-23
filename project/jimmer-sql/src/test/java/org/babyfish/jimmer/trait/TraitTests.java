package org.babyfish.jimmer.trait;

import lombok.SneakyThrows;
import org.babyfish.jimmer.sql.model.SysUser;
import org.babyfish.jimmer.sql.model.SysUserDraft;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

/**
 * @author honhimW
 * @since 2025-10-23
 */

public class TraitTests {

    @Test
    @SneakyThrows
    void traitIgnored() {
        SysUser produce = SysUserDraft.$.produce(draft -> {
        });
        Function<String, String> fuck = produce.fuck("B");
        String apply = fuck.apply("A");
        assert "A fuck B".equals(apply);
    }

}
