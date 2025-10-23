package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.table.WeakJoin;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import static org.babyfish.jimmer.sql.common.Constants.*;

import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

public class JoinTest extends AbstractQueryTest {

    @Test
    public void testSimple() {

        executeAndExpect(
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    return q.select(book);
                }),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_"
                    );
                }
        );
    }

    @Test
    public void testUnused() {
        executeAndExpect(
                getLambdaClient().createQuery(BookStoreTable.class, (q, store) -> {
                    store.asTableEx().books();
                    return q.select(store);
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                                    "from BOOK_STORE tb_1_"
                    );
                }
        );
        executeAndExpect(
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    ((BookTableEx)book).authors();
                    return q.select(book);
                }),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_"
                    );
                }
        );
    }

    @Test
    public void testMergedJoinFromParentToChild() {
        executeAndExpect(
                getLambdaClient().createQuery(BookStoreTable.class, (q, store) -> {
                    q.where(
                            store.asTableEx().books(JoinType.LEFT).price()
                                    .ge(new BigDecimal(20))
                    );
                    q.where(
                            store.asTableEx().books().price()
                                    .le(new BigDecimal(30))
                    );
                    q.where(
                            store
                                    .asTableEx()
                                    .books()
                                    .authors()
                                    .firstName()
                                    .ilike("Alex")
                    );
                    return q.select(Expression.constant(1));
                }),
                ctx -> {
                    ctx.sql(
                            "select 1 " +
                                    "from BOOK_STORE tb_1_ " +
                                    "inner join BOOK tb_2_ on tb_1_.ID = tb_2_.STORE_ID " +
                                    "inner join BOOK_AUTHOR_MAPPING tb_3_ on tb_2_.ID = tb_3_.BOOK_ID " +
                                    "inner join AUTHOR tb_4_ on tb_3_.AUTHOR_ID = tb_4_.ID " +
                                    "where tb_2_.PRICE >= ? and tb_2_.PRICE <= ? and tb_4_.FIRST_NAME ilike ?"
                    );
                    ctx.variables(
                            new BigDecimal(20), new BigDecimal(30), "%alex%"
                    );
                }
        );
    }

    @Test
    public void testMergedJoinFromChildToParent() {
        executeAndExpect(
                getLambdaClient().createQuery(AuthorTable.class, (q, author) -> {
                    q.where(
                            author
                                    .asTableEx()
                                    .books(JoinType.LEFT)
                                    .price()
                                    .ge(new BigDecimal(20))
                    );
                    q.where(
                            author
                                    .asTableEx()
                                    .books()
                                    .price()
                                    .le(new BigDecimal(30))
                    );
                    q.where(
                            author
                                    .asTableEx()
                                    .books()
                                    .store()
                                    .name()
                                    .ilike("MANNING")
                    );
                    return q.select(Expression.constant(1));
                }),
                ctx -> {
                    ctx.sql(
                            "select 1 " +
                                    "from AUTHOR tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "inner join BOOK tb_3_ on tb_2_.BOOK_ID = tb_3_.ID " +
                                    "inner join BOOK_STORE tb_4_ on tb_3_.STORE_ID = tb_4_.ID " +
                                    "where tb_3_.PRICE >= ? and tb_3_.PRICE <= ? and tb_4_.NAME ilike ?"
                    );
                    ctx.variables(new BigDecimal(20), new BigDecimal(30), "%manning%");
                }
        );
    }

    @Test
    public void testUnnecessaryJoin() {
        executeAndExpect(
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    q.where(
                            book.store().id().in(Arrays.asList(oreillyId, manningId))
                    );
                    return q.select(Expression.constant(1));
                }),
                ctx -> {
                    ctx.sql(
                            "select 1 from BOOK tb_1_ where tb_1_.STORE_ID in (?, ?)"
                    );
                    ctx.variables(oreillyId, manningId);
                }
        );
    }

    @Test
    public void testUnnecessaryJoinByIdView() {
        executeAndExpect(
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    q.where(
                            book.storeId().in(Arrays.asList(oreillyId, manningId))
                    );
                    return q.select(Expression.constant(1));
                }),
                ctx -> {
                    ctx.sql(
                            "select 1 from BOOK tb_1_ where tb_1_.STORE_ID in (?, ?)"
                    );
                    ctx.variables(oreillyId, manningId);
                }
        );
    }

    @Test
    public void testHalfJoin() {
        executeAndExpect(
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    q.where(
                            book
                                    .asTableEx()
                                    .authors()
                                    .id()
                                    .in(Arrays.asList(alexId, borisId))
                    );
                    return q.select(Expression.constant(1));
                }),
                ctx -> {
                    ctx.sql(
                            "select 1 " +
                                    "from BOOK tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.BOOK_ID " +
                                    "where tb_2_.AUTHOR_ID in (?, ?)"
                    );
                    ctx.variables(alexId, borisId);
                }
        );
    }

    @Test
    public void testHalfInverseJoin() {
        executeAndExpect(
                getLambdaClient().createQuery(AuthorTable.class, (q, author) -> {
                    q.where(
                            author
                                    .asTableEx()
                                    .books()
                                    .id()
                                    .in(Arrays.asList(learningGraphQLId1, learningGraphQLId2))
                    );
                    return q.select(Expression.constant(1));
                }),
                ctx -> {
                    ctx.sql(
                            "select 1 " +
                                    "from AUTHOR tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "where tb_2_.BOOK_ID in (?, ?)"
                    );
                    ctx.variables(learningGraphQLId1, learningGraphQLId2);
                }
        );
    }

    @Test
    public void testOneToManyCannotBeOptimized() {
        executeAndExpect(
                getLambdaClient().createQuery(BookStoreTable.class, (q, store) -> {
                    q.where(
                            store
                                    .asTableEx()
                                    .books()
                                    .id()
                                    .in(Arrays.asList(learningGraphQLId1, learningGraphQLId2))
                    );
                    return q.select(Expression.constant(1));
                }),
                ctx -> {
                    ctx.sql(
                            "select 1 " +
                                    "from BOOK_STORE tb_1_ " +
                                    "inner join BOOK tb_2_ on tb_1_.ID = tb_2_.STORE_ID " +
                                    "where tb_2_.ID in (?, ?)"
                    );
                    ctx.variables(learningGraphQLId1, learningGraphQLId2);
                }
        );
    }

    @Test
    public void testOuterJoin() {
        executeAndExpect(
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    q.where(
                            Predicate.or(
                                    book.store(JoinType.LEFT).id().isNotNull(),
                                    book.store(JoinType.LEFT).name().ilike("MANNING")
                            )
                    );
                    return q.select(Expression.constant(1));
                }),
                ctx -> {
                    ctx.sql(
                            "select 1 " +
                                    "from BOOK tb_1_ " +
                                    "left join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID " +
                                    "where tb_1_.STORE_ID is not null " +
                                    "or tb_2_.NAME ilike ?"
                    );
                    ctx.variables("%manning%");
                }
        );
    }

    @Test
    public void testUnusedWeakJoin() {
        executeAndExpect(
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    q.where(book.id().eq(graphQLInActionId3));
                    book.asTableEx().weakJoin(BookAuthorWeakJoin.class);
                    return q.select(book);
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.ID = ?"
                    );
                }
        );
    }

    @Test
    public void testWeakJoin() {
        executeAndExpect(
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    q.where(book.asTableEx().weakJoin(BookAuthorWeakJoin.class).firstName().eq("Alex"));
                    return q.select(book);
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "inner join AUTHOR tb_2_ on exists(" +
                                    "--->select * from book_author_mapping " +
                                    "--->where book_id = tb_1_.ID and author_id = tb_2_.ID" +
                                    ") " +
                                    "where tb_2_.FIRST_NAME = ?"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"price\":50.00," +
                                    "--->--->\"storeId\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "--->},{" +
                                    "--->--->\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":2," +
                                    "--->--->\"price\":55.00," +
                                    "--->--->\"storeId\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "--->},{" +
                                    "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":51.00," +
                                    "--->--->\"storeId\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testMergeWeakJoin() {
        executeAndExpect(
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    q.where(book.asTableEx().weakJoin(BookAuthorWeakJoin.class).firstName().eq("Alex"));
                    q.where(book.asTableEx().weakJoin(BookAuthorWeakJoin.class).lastName().eq("Banks"));
                    return q.select(book);
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "inner join AUTHOR tb_2_ on exists(" +
                                    "--->select * from book_author_mapping " +
                                    "--->where book_id = tb_1_.ID and author_id = tb_2_.ID" +
                                    ") " +
                                    "where tb_2_.FIRST_NAME = ? and tb_2_.LAST_NAME = ?"
                    ).variables("Alex", "Banks");
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"price\":50.00," +
                                    "--->--->\"storeId\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "--->},{" +
                                    "--->--->\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":2," +
                                    "--->--->\"price\":55.00," +
                                    "--->--->\"storeId\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "--->},{" +
                                    "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":51.00," +
                                    "--->--->\"storeId\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testMergeLambdaWeakJoin() {
        executeAndExpect(
                getLambdaClient().createQuery(BookTable.class, (q, book) -> {
                    q.where(
                            book.asTableEx().weakJoin(AuthorTable.class, (b, a) ->
                                    Predicate.sql(
                                            "exists(select * from book_author_mapping where book_id = %e and author_id = %e)",
                                            ctx -> {
                                                ctx.expression(b.id());
                                                ctx.expression(a.id());
                                            }
                                    )
                            ).firstName().eq("Alex")
                    );
                    q.where(
                            book.asTableEx().weakJoin(AuthorTable.class, (b2, a2) ->
                                    Predicate.sql(
                                            "exists(select * from book_author_mapping where book_id = %e and author_id = %e)",
                                            ctx -> {
                                                ctx.expression(b2.id());
                                                ctx.expression(a2.id());
                                            }
                                    )
                            ).lastName().eq("Banks")
                    );
                    return q.select(book);
                }),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "inner join AUTHOR tb_2_ on exists(" +
                                    "--->select * from book_author_mapping " +
                                    "--->where book_id = tb_1_.ID and author_id = tb_2_.ID" +
                                    ") " +
                                    "where tb_2_.FIRST_NAME = ? and tb_2_.LAST_NAME = ?"
                    ).variables("Alex", "Banks");
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"price\":50.00," +
                                    "--->--->\"storeId\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "--->},{" +
                                    "--->--->\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":2," +
                                    "--->--->\"price\":55.00," +
                                    "--->--->\"storeId\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "--->},{" +
                                    "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":51.00," +
                                    "--->--->\"storeId\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    private static class BookAuthorWeakJoin implements WeakJoin<BookTable, AuthorTable> {

        @Override
        public Predicate on(BookTable source, AuthorTable target) {
            return Predicate.sql(
                    "exists(select * from book_author_mapping where book_id = %e and author_id = %e)",
                    source.id(),
                    target.id()
            );
        }
    }

    @Test
    public void testIssue1176() {
        NativeDatabases.assumeNativeDatabase();
        AuthorTable table = AuthorTable.$;
        BookTable book1 = table.asTableEx().weakJoin(BookTable.class, JoinType.LEFT, (s, t) -> {
           return s.firstName().eq(t.name());
        });
        BookTable book2 = table.asTableEx().weakJoin(BookTable.class, JoinType.LEFT, (s, t) -> {
            return s.lastName().eq(t.name());
        });
        executeAndExpect(
                NativeDatabases.MYSQL_DATA_SOURCE,
                getSqlClient()
                        .createQuery(table)
                        .select(book2.name(), book1.name()),
                ctx -> {
                    ctx.sql(
                            "select tb_3_.NAME, tb_2_.NAME " +
                                    "from AUTHOR tb_1_ " +
                                    "left join BOOK tb_2_ on tb_1_.FIRST_NAME = tb_2_.NAME " +
                                    "left join BOOK tb_3_ on tb_1_.LAST_NAME = tb_3_.NAME"
                    );
                }
        );
    }
}
