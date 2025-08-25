package org.babyfish.jimmer.sql.ddl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ddl.anno.Relation;

/**
 * @author honhimW
 */

public class ForeignKey {

    public final Relation relation;

    public final ImmutableProp joinColumn;

    public final ImmutableType table;

    public final ImmutableType referencedTable;

    public ForeignKey(Relation relation, ImmutableProp joinColumn, ImmutableType table, ImmutableType referencedTable) {
        this.relation = relation;
        this.joinColumn = joinColumn;
        this.table = table;
        this.referencedTable = referencedTable;
    }
}
