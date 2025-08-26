package org.babyfish.jimmer.sql.trait;

import org.babyfish.jimmer.sql.model.inheritance.Administrator;
import org.babyfish.jimmer.sql.model.inheritance.AdministratorDraft;
import org.junit.jupiter.api.Test;

/**
 * @author honhimW
 * @since 2025-08-22
 */

public class TraitMethodTest {

    @Test
    void traitOnEntity() {
        Administrator administrator = AdministratorDraft.$.produce(a -> {});
        String s = administrator.onAdministrator();
        assert "onAdministrator".equals(s);
    }

    @Test
    void traitOnSupperClass() {
        Administrator administrator = AdministratorDraft.$.produce(a -> {});
        {
            String s = administrator.onAdministratorBase();
            assert "onAdministratorBase".equals(s);
        }
        {
            String s = administrator.onNamedEntity();
            assert "onNamedEntity".equals(s);
        }
        {
            String s = administrator.onUserInfo();
            assert "onUserInfo".equals(s);
        }
    }

    @Test
    void traitToBeOverride() {
        Administrator administrator = AdministratorDraft.$.produce(a -> {});
        String s = administrator.toBeOverride();
        assert "override by administrator".equals(s);
    }

}
