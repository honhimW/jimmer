package org.babyfish.jimmer.sql.ddl;

import org.apache.commons.lang3.StringUtils;
import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.EnumType;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.ddl.annotations.*;
import org.babyfish.jimmer.sql.ddl.dialect.DDLDialect;
import org.babyfish.jimmer.sql.meta.EmbeddedColumns;
import org.babyfish.jimmer.sql.meta.SingleColumn;
import org.babyfish.jimmer.sql.meta.Storage;
import org.babyfish.jimmer.sql.meta.UserIdGenerator;
import org.babyfish.jimmer.sql.meta.impl.Storages;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.lang.reflect.Field;
import java.sql.DatabaseMetaData;
import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author honhimW
 */

public class StandardTableExporter implements Exporter<ImmutableType> {

    protected final JSqlClientImplementor client;

    protected final DDLDialect dialect;

    private EnumType.Strategy defaultEnumStrategy;

    public StandardTableExporter(JSqlClientImplementor client) {
        this.client = client;
        DatabaseVersion databaseVersion = client.getConnectionManager().execute(connection -> {
            try {
                DatabaseMetaData metaData = connection.getMetaData();
                int databaseMajorVersion = metaData.getDatabaseMajorVersion();
                int databaseMinorVersion = metaData.getDatabaseMinorVersion();
                String databaseProductVersion = metaData.getDatabaseProductVersion();
                return new DatabaseVersion(databaseMajorVersion, databaseMinorVersion, databaseProductVersion);
            } catch (Exception e) {
                DDLUtils.DDL_LOGGER.warn("cannot get database version, using latest as default", e);
                return new DatabaseVersion(Integer.MAX_VALUE, Integer.MAX_VALUE, "unknown");
            }
        });
        this.dialect = DDLDialect.of(client.getDialect(), databaseVersion);
    }

    public StandardTableExporter(JSqlClientImplementor client, DatabaseVersion version) {
        this.client = client;
        this.dialect = DDLDialect.of(client.getDialect(), version);
    }

    @Override
    public List<String> getSqlCreateStrings(ImmutableType exportable) {
        BufferContext bufferContext = new BufferContext(client, exportable);
        final List<String> statements = new ArrayList<>();

        bufferContext.buf.append("create table ").append(bufferContext.tableName).append(" (");
        if (isPretty()) {
            bufferContext.buf.append('\n').append(client.getSqlFormatter().getIndent());
        }

        appendColumns(bufferContext);

        // Jimmer always has primary-key
        appendPrimaryKey(bufferContext);

        appendUniqueConstraints(bufferContext);

        appendTableCheck(bufferContext);

        if (isPretty()) {
            bufferContext.buf.append('\n');
        }
        bufferContext.buf.append(")");

        appendTableComment(bufferContext);

        appendTableType(bufferContext);

        statements.add(bufferContext.buf.toString());

        applyComments(bufferContext, statements);

        applyIndexes(bufferContext, statements);

        return statements;
    }

    @Override
    public List<String> getSqlDropStrings(ImmutableType exportable) {
        BufferContext bufferContext = new BufferContext(client, exportable);
        bufferContext.buf.append("drop table ");
        if (dialect.supportsIfExistsBeforeTableName()) {
            bufferContext.buf.append("if exists ");
        }
        bufferContext.buf.append(bufferContext.tableName).append(' ').append(dialect.getCascadeConstraintsString());
        if (dialect.supportsIfExistsAfterTableName()) {
            bufferContext.buf.append(" if exists");
        }
        List<String> statements = new ArrayList<>();
        statements.add(bufferContext.buf.toString());
        return statements;
    }

    private void applyComments(BufferContext bufferContext, List<String> statements) {
        if (dialect.supportsCommentOn()) {
            String comment = bufferContext.getTableDef().map(TableDef::comment).orElse(null);
            if (StringUtils.isNotBlank(comment) && StringUtils.isBlank(dialect.getTableComment(""))) {
                statements.add(String.format("comment on table %s is '%s'", bufferContext.tableName, comment));
            }
            statements.addAll(bufferContext.commentStatements);
        }
    }

    private void applyIndexes(BufferContext bufferContext, List<String> statements) {
        Optional<TableDef> optional = bufferContext.getTableDef();
        if (optional.isPresent()) {
            TableDef tableDef = optional.get();
            Index[] indexes = tableDef.indexes();
            for (Index index : indexes) {
                StringBuilder buf = new StringBuilder();
                buf.append(dialect.getCreateIndexString(index.unique())).append(' ');
                if (StringUtils.isNotBlank(index.name())) {
                    buf.append(index.name());
                } else {
                    ConstraintNamingStrategy namingStrategy = bufferContext.getNamingStrategy(index.naming());
                    buf.append(namingStrategy.determineIndexName(bufferContext.tableName, index.columns()));
                }
                buf
                    .append(" on ")
                    .append(bufferContext.tableName)
                    .append(" (");
                appendIndexColumnList(bufferContext, buf, index);
                buf.append(')');
                statements.add(buf.toString());
            }

        }
    }

    private void appendIndexColumnList(BufferContext bufferContext, StringBuilder idxBuf, Index index) {
        boolean isFirst = true;
        String[] columns = index.columns();
        Kind kind = index.kind();
        for (String column : columns) {
            if (isFirst) {
                isFirst = false;
            } else {
                idxBuf.append(", ");
            }
            switch (kind) {
                case PATH:
                    ImmutableProp immutableProp = bufferContext.allDefinitionProps.get(column);
                    if (immutableProp != null) {
                        idxBuf.append(dialect.quote(DDLUtils.getName(immutableProp, client.getMetadataStrategy())));
                    } else {
                        throw new IllegalArgumentException("unknown column in constraint define: " + column);
                    }
                    break;
                case NAME:
                    idxBuf.append(dialect.quote(column));
                    break;
            }
        }
    }

    private void appendTableComment(BufferContext bufferContext) {
        bufferContext.getTableDef().ifPresent(tableDef -> {
            String comment = tableDef.comment();
            if (StringUtils.isNotBlank(comment)) {
                bufferContext.buf.append(dialect.getTableComment(comment));
            }
        });
    }

    private void appendTableType(BufferContext bufferContext) {
        bufferContext.getTableDef().ifPresent(tableDef -> {
            String tableType = tableDef.tableType();
            if (StringUtils.isNotBlank(tableType)) {
                bufferContext.buf.append(' ').append(tableType);
            }
            String tableTypeString = dialect.getTableTypeString();
            if (StringUtils.isNotBlank(tableTypeString)) {
                bufferContext.buf.append(' ').append(tableTypeString);
            }
        });

    }

    private void appendTableCheck(BufferContext bufferContext) {
        if (dialect.supportsTableCheck()) {
            bufferContext.getTableDef().ifPresent(tableDef -> {
                Check[] checks = tableDef.checks();
                for (Check check : checks) {
                    bufferContext.buf.append(',');
                    if (isPretty()) {
                        bufferContext.buf.append('\n').append(client.getSqlFormatter().getIndent());
                    } else {
                        bufferContext.buf.append(' ');
                    }
                    String name = check.name();
                    if (StringUtils.isNotBlank(name)) {
                        bufferContext.buf.append("constraint ").append(name).append(' ');
                    }
                    String constraint = check.constraint();
                    bufferContext.buf.append("check (").append(constraint).append(")");
                }
            });
        }
    }

    private boolean isPretty() {
        return client.getSqlFormatter().isPretty();
    }

    @SuppressWarnings("unused")
    private List<SingleColumn> resolveColumns(ImmutableProp prop) {
        List<SingleColumn> list = new ArrayList<>();
        Storage storage = Storages.of(prop, client.getMetadataStrategy());
        if (storage instanceof SingleColumn) {
            SingleColumn singleColumn = (SingleColumn) storage;
            list.add(singleColumn);
        } else if (storage instanceof EmbeddedColumns) {
            EmbeddedColumns embeddedColumns = (EmbeddedColumns) storage;
            for (String embeddedColumn : embeddedColumns) {
                List<ImmutableProp> path = embeddedColumns.path(embeddedColumn);
                ImmutableProp last = path.get(path.size() - 1);
                list.addAll(resolveColumns(last));
            }
        }
        return list;
    }

    private void appendColumns(BufferContext bufferContext) {
        Map<String, ImmutableProp> selectableProps = bufferContext.tableType.getSelectableProps();
        boolean isFirst = true;
        for (Map.Entry<String, ImmutableProp> entry : selectableProps.entrySet()) {
            ImmutableProp selectableProp = entry.getValue();

            List<ImmutableProp> props = new ArrayList<>();
            if (selectableProp.isEmbedded(EmbeddedLevel.BOTH)) {
                ImmutableType targetType = selectableProp.getTargetType();
                Map<String, ImmutableProp> allDefinitionProps = DDLUtils.allDefinitionProps(targetType);
                props.addAll(allDefinitionProps.values());
            } else {
                props.add(selectableProp);
            }

            for (ImmutableProp prop : props) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    bufferContext.buf.append(',');
                    if (isPretty()) {
                        bufferContext.buf.append('\n').append(client.getSqlFormatter().getIndent());
                    } else {
                        bufferContext.buf.append(' ');
                    }
                }
                if (prop.isId()) {
                    ImmutableProp idProp = bufferContext.tableType.getIdProp();
                    GeneratedValue annotation = idProp.getAnnotation(GeneratedValue.class);
                    if (annotation != null) {
                        if (annotation.generatorType() == UserIdGenerator.None.class) {
                            appendColumn(bufferContext, prop, true);
                            continue;
                        }
                    }
                }
                appendColumn(bufferContext, prop, false);
            }

        }
    }

    private void appendColumn(BufferContext bufferContext, ImmutableProp prop, boolean isId) {
        if (prop.isColumnDefinition()) {
            appendColumnDefinition(bufferContext, prop, isId);
            appendComment(bufferContext, prop);
            appendConstraints(bufferContext, prop);
        }
    }

    private void appendColumnDefinition(BufferContext bufferContext, ImmutableProp prop, boolean isId) {
        bufferContext.buf.append(dialect.quote(getName(prop)));
        ColumnDef colDef = prop.getAnnotation(ColumnDef.class);
        String columnType;
        EnumType.Strategy strategy = resolveEnumStrategy(prop);

        boolean nullable = prop.isNullable();

        if (prop.isReference(TargetLevel.PERSISTENT)) {
            prop = prop.getTargetType().getIdProp();
        }

        int jdbcType = dialect.resolveJdbcType(prop.getReturnClass(), strategy);
        if (colDef != null) {
            if (StringUtils.isNotBlank(colDef.definition())) {
                bufferContext.buf.append(' ').append(colDef.definition());
                return;
            }
            switch (colDef.nullable()) {
                case TRUE:
                    nullable = true;
                    break;
                case FALSE:
                    nullable = false;
                    break;
            }
            if (colDef.jdbcType() != Types.OTHER) {
                jdbcType = colDef.jdbcType();
            }
            columnType = dialect.columnType(jdbcType, colDef.length(), colDef.precision(), colDef.scale());
            String sqlType = colDef.sqlType();
            if (StringUtils.isNotBlank(sqlType)) {
                columnType = DDLUtils.replace(sqlType, colDef.length(), colDef.precision(), colDef.scale());
            }
        } else {
            columnType = dialect.columnType(jdbcType, null, null, null);
        }
        if (isId) {
            if (dialect.hasDataTypeInIdentityColumn()) {
                bufferContext.buf.append(' ').append(columnType);
            }
            bufferContext.buf.append(' ').append(dialect.getIdentityColumnString(jdbcType));
        } else {
            bufferContext.buf.append(' ').append(columnType);

            Ref<Object> defaultValueRef = prop.getDefaultValueRef();
            if (defaultValueRef != null) {
                bufferContext.buf.append(" default ").append(defaultValueRef.getValue());
            }

            if (nullable) {
                bufferContext.buf.append(dialect.getNullColumnString());
            } else {
                bufferContext.buf.append(" not null");
            }
        }
    }

    private void appendComment(BufferContext bufferContext, ImmutableProp prop) {
        ColumnDef colDef = prop.getAnnotation(ColumnDef.class);
        if (colDef != null) {
            String comment = colDef.comment();
            if (StringUtils.isNotBlank(comment)) {
                String columnComment = dialect.getColumnComment(comment);
                if (StringUtils.isNotBlank(columnComment)) {
                    bufferContext.buf.append(columnComment);
                } else {
                    bufferContext.commentStatements.add(String.format("comment on column %s.%s is '%s'", bufferContext.tableType.getTableName(client.getMetadataStrategy()), dialect.quote(getName(prop)), comment));
                }
            }
        }
    }

    private void appendConstraints(BufferContext bufferContext, ImmutableProp prop) {
        if (dialect.supportsColumnCheck()) {
            Class<?> returnClass = prop.getReturnClass();
            if (returnClass.isEnum()) {
                EnumType.Strategy strategy = resolveEnumStrategy(prop);
                Enum<?>[] enumConstants = (Enum<?>[]) returnClass.getEnumConstants();
                if (enumConstants.length > 0) {
                    String checkCondition;
                    switch (strategy) {
                        case ORDINAL:
                            checkCondition = dialect.getCheckCondition(getName(prop), 0, enumConstants.length - 1);
                            break;
                        case NAME:
                        default:
                            List<String> names = Arrays.stream(enumConstants).map(Enum::name).collect(Collectors.toList());
                            if (prop.isNullable()) {
                                names.add(null);
                            }
                            checkCondition = dialect.getCheckCondition(getName(prop), names);
                            break;

                    }
                    bufferContext.buf.append(" check (").append(checkCondition).append(')');
                }

            }
        }
    }

    private void appendPrimaryKey(BufferContext bufferContext) {
        ImmutableProp idProp = bufferContext.tableType.getIdProp();
        bufferContext.buf.append(',');
        if (isPretty()) {
            bufferContext.buf.append('\n').append(client.getSqlFormatter().getIndent());
        } else {
            bufferContext.buf.append(' ');
        }
        List<ImmutableProp> props = new ArrayList<>();
        if (idProp.isEmbedded(EmbeddedLevel.BOTH)) {
            ImmutableType targetType = idProp.getTargetType();
            Map<String, ImmutableProp> allDefinitionProps = DDLUtils.allDefinitionProps(targetType);
            props.addAll(allDefinitionProps.values());
        } else {
            props.add(idProp);
        }
        bufferContext.buf.append("primary key (");
        boolean isFirst = true;
        for (ImmutableProp prop : props) {
            if (isFirst) {
                isFirst = false;
            } else {
                bufferContext.buf.append(", ");
            }
            bufferContext.buf.append(dialect.quote(getName(prop)));
        }

        bufferContext.buf.append(')');
    }

    private void appendUniqueConstraints(BufferContext bufferContext) {
        bufferContext.getTableDef().ifPresent(tableDef -> {
            Unique[] uniques = tableDef.uniques();
            for (Unique unique : uniques) {
                if (unique.columns().length > 0) {
                    appendUniqueConstraint(bufferContext, unique);
                }
            }
        });
    }

    private void appendUniqueConstraint(BufferContext bufferContext, Unique unique) {
        bufferContext.buf.append(',');
        if (isPretty()) {
            bufferContext.buf.append('\n').append(client.getSqlFormatter().getIndent());
        } else {
            bufferContext.buf.append(' ');
        }
        bufferContext.buf.append("constraint ");
        if (StringUtils.isNotBlank(unique.name())) {
            bufferContext.buf.append(unique.name());
        } else {
            ConstraintNamingStrategy namingStrategy = bufferContext.getNamingStrategy(unique.naming());
            bufferContext.buf.append(namingStrategy.determineUniqueKeyName(bufferContext.tableName, unique.columns()));
        }
        bufferContext.buf.append(" unique (");
        Kind kind = unique.kind();
        String[] columns = unique.columns();
        boolean isFirst = true;
        for (String column : columns) {
            if (isFirst) {
                isFirst = false;
            } else {
                bufferContext.buf.append(", ");
            }
            switch (kind) {
                case PATH:
                    ImmutableProp immutableProp = bufferContext.allDefinitionProps.get(column);
                    if (immutableProp != null) {
                        bufferContext.buf.append(dialect.quote(getName(immutableProp)));
                    } else {
                        throw new IllegalArgumentException("unknown column in constraint define: " + column);
                    }
                    break;
                case NAME:
                    bufferContext.buf.append(dialect.quote(column));
                    break;
            }
        }
        bufferContext.buf.append(')');
    }

    private String getName(ImmutableProp prop) {
        return DDLUtils.getName(prop, client.getMetadataStrategy());
    }

    private EnumType.Strategy resolveEnumStrategy(ImmutableProp prop) {
        EnumType.Strategy strategy = EnumType.Strategy.NAME;
        if (prop.getReturnClass().isAnnotationPresent(EnumType.class)) {
            EnumType annotation = prop.getReturnClass().getAnnotation(EnumType.class);
            assert annotation != null;
            strategy = annotation.value();
        } else {
            try {
                if (this.defaultEnumStrategy == null) {
                    Class<?> scalarProviderManager = Class.forName("org.babyfish.jimmer.sql.ScalarProviderManager");
                    Field defaultEnumStrategy = scalarProviderManager.getDeclaredField("defaultEnumStrategy");
                    defaultEnumStrategy.setAccessible(true);
                    this.defaultEnumStrategy = (EnumType.Strategy) defaultEnumStrategy.get(client.getMetadataStrategy().getScalarTypeStrategy());
                }
                strategy = this.defaultEnumStrategy;
            } catch (Exception e) {
                DDLUtils.DDL_LOGGER.warn("cannot get default-enum-strategy: {}", e.getMessage());
            }
        }
        return strategy;
    }

}
