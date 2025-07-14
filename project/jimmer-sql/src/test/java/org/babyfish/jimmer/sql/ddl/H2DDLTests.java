package org.babyfish.jimmer.sql.ddl;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.impl.table.TableTypeProvider;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.model.Tables;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author honhimW
 */

public class H2DDLTests extends AbstractDDLTest {

    @Test
    void create() {
        JSqlClientImplementor sqlClient = (JSqlClientImplementor) getSqlClient();
        SchemaCreator schemaCreator = new SchemaCreator(sqlClient, DatabaseVersion.LATEST);
        schemaCreator.init();
        List<Table<?>> tables = new ArrayList<>();
        tables.add(Tables.AUTHOR_TABLE);
        tables.add(Tables.BOOK_TABLE);
        tables.add(Tables.COUNTRY_TABLE);
        tables.add(Tables.ORGANIZATION_TABLE);
        List<ImmutableType> types = tables.stream().map(TableTypeProvider::getImmutableType).collect(Collectors.toList());
        List<String> sqlCreateStrings = schemaCreator.getSqlCreateStrings(types);
        for (String sqlCreateString : sqlCreateStrings) {
            System.out.println(sqlCreateString);
            getExecutions().add(Execution.simple(sqlCreateString, ExecutionPurpose.QUERY, Collections.emptyList()));
        }

    }

}
