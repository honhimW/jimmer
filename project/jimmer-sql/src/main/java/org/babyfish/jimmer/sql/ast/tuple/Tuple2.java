package org.babyfish.jimmer.sql.ast.tuple;

import org.babyfish.jimmer.sql.ast.impl.TupleImplementor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author honhimW
 * @since 2025-11-10
 */

public class Tuple2<T1, T2> implements TupleImplementor {

    private final T1 _1;
    private final T2 _2;

    public Tuple2(T1 _1, T2 _2) {
        this._1 = _1;
        this._2 = _2;
    }

    @Override
    public int size() {
        return 2;
    }

    @Override
    public Object get(int index) {
        switch (index) {
            case 0:
                return _1;
            case 1:
                return _2;
            default:
                throw new IllegalArgumentException(String.format("Index must between 0 and %d", size() - 1));
        }
    }

    @Override
    public TupleImplementor convert(BiFunction<Object, Integer, Object> block) {
        return new Tuple2<>(block.apply(_1, 0), block.apply(_2, 1));
    }

    public T1 get_1() {
        return _1;
    }

    public T2 get_2() {
        return _2;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Tuple2<?, ?> tuple2 = (Tuple2<?, ?>) o;
        return Objects.equals(_1, tuple2._1) && Objects.equals(_2, tuple2._2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_1, _2);
    }

    @Override
    public String toString() {
        return "Tuple2(" +
               "_1=" + _1 +
               ", _2=" + _2 +
               ')';
    }

    public static <K, V> Map<K, V> toMap(Collection<Tuple2<K, V>> tuples) {
        return tuples.stream().collect(Collectors.toMap(kvTuple2 -> kvTuple2._1, kvTuple2 -> kvTuple2._2));
    }

    public static <K, T, V> Map<K, V> toMap(Collection<Tuple2<K, T>> tuples, Function<T, V> valueMapper) {
        return tuples.stream().collect(Collectors.toMap(kvTuple2 -> kvTuple2._1, kvTuple2 -> valueMapper.apply(kvTuple2._2)));
    }

    public static <K, V> Map<K, List<V>> toMultiMap(Collection<Tuple2<K, V>> tuples) {
        return tuples.stream().collect(
            Collectors.groupingBy(
                (Tuple2<K, V> kvTuple2) -> kvTuple2._1,
                Collectors.mapping(kvTuple2 -> kvTuple2._2, Collectors.toList())
            ));
    }

    public static <K, T, V> Map<K, List<V>> toMultiMap(Collection<Tuple2<K, T>> tuples, Function<T, V> valueMapper) {
        return tuples.stream().collect(
            Collectors.groupingBy(
                (Tuple2<K, T> kvTuple2) -> kvTuple2._1,
                Collectors.mapping((Tuple2<K, T> kvTuple2) -> valueMapper.apply(kvTuple2._2), Collectors.toList())
            ));
    }

}
