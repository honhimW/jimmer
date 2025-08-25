package org.babyfish.jimmer.sql.ddl;

import lombok.SneakyThrows;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.impl.table.TableTypeProvider;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.model.Tables;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author honhimW
 * @since 2025-08-25
 */

public class UtilTests extends AbstractDDLTest {

    @Test
    @SneakyThrows
    void sortImmutableTypeByDependent() {
        JSqlClientImplementor sqlClient = getSqlClient();
        List<Table<?>> tables = new ArrayList<>();
        tables.add(Tables.COUNTRY_TABLE);
        tables.add(Tables.BOOK_TABLE);
        tables.add(Tables.ORGANIZATION_TABLE);
        tables.add(Tables.AUTHOR_TABLE);
        tables.add(Tables.BOOK_STORE_TABLE);
        tables.add(Tables.CARD_TABLE);

//        tables.add(Tables.AUTHOR_TABLE);
//        tables.add(Tables.BOOK_STORE_TABLE);
//        tables.add(Tables.BOOK_TABLE);
//        tables.add(Tables.COUNTRY_TABLE);
//        tables.add(Tables.ORGANIZATION_TABLE);

        List<ImmutableType> types = tables.stream().map(TableTypeProvider::getImmutableType).collect(Collectors.toList());
        types = DDLUtils.sortByDependent(sqlClient.getMetadataStrategy(), types);
        for (ImmutableType type : types) {
            System.out.println(type.getTableName(jSqlClientImplementor.getMetadataStrategy()));
        }
        Assertions.assertEquals(types.size(), tables.size());
        Assertions.assertTrue(types.indexOf(Tables.COUNTRY_TABLE.getImmutableType()) < types.indexOf(Tables.AUTHOR_TABLE.getImmutableType()));
        Assertions.assertTrue(types.indexOf(Tables.ORGANIZATION_TABLE.getImmutableType()) < types.indexOf(Tables.AUTHOR_TABLE.getImmutableType()));
        Assertions.assertTrue(types.indexOf(Tables.BOOK_STORE_TABLE.getImmutableType()) < types.indexOf(Tables.BOOK_TABLE.getImmutableType()));
        Assertions.assertTrue(types.contains(Tables.CARD_TABLE.getImmutableType()));
    }

}
