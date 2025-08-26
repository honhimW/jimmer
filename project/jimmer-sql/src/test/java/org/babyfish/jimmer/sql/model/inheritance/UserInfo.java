package org.babyfish.jimmer.sql.model.inheritance;

import org.babyfish.jimmer.sql.MappedSuperclass;
import org.babyfish.jimmer.sql.Trait;

@MappedSuperclass
public interface UserInfo {

    String name();

    @Trait
    default String onUserInfo() {
        return "onUserInfo";
    }
}
