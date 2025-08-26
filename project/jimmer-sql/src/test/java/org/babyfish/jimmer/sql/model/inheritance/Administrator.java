package org.babyfish.jimmer.sql.model.inheritance;

import org.babyfish.jimmer.sql.*;

@Entity
@KeyUniqueConstraint
public interface Administrator extends AdministratorBase, UserInfo {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    long getId();

    @Trait
    default String onAdministrator() {
        return "onAdministrator";
    }

    @Trait
    @Override
    default String toBeOverride() {
        return "override by administrator";
    }
}
