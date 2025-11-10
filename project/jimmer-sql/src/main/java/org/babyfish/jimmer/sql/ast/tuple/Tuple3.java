package org.babyfish.jimmer.sql.ast.tuple;

import org.babyfish.jimmer.sql.ast.impl.TupleImplementor;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * @author honhimW
 * @since 2025-11-10
 */

public class Tuple3<T1, T2, T3> implements TupleImplementor {

    private final T1 _1;
    private final T2 _2;
    private final T3 _3;

    public Tuple3(T1 _1, T2 _2, T3 t3) {
        this._1 = _1;
        this._2 = _2;
        this._3 = t3;
    }

    @Override
    public int size() {
        return 3;
    }

    @Override
    public Object get(int index) {
        switch (index) {
            case 0:
                return _1;
            case 1:
                return _2;
            case 2:
                return _3;
            default:
                throw new IllegalArgumentException(String.format("Index must between 0 and %d", size() - 1));
        }
    }

    @Override
    public TupleImplementor convert(BiFunction<Object, Integer, Object> block) {
        return new Tuple3<>(
            block.apply(_1, 0),
            block.apply(_2, 1),
            block.apply(_3, 2)
        );
    }

    public T1 get_1() {
        return _1;
    }

    public T2 get_2() {
        return _2;
    }

    public T3 get_3() {
        return _3;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Tuple3<?, ?, ?> tuple3 = (Tuple3<?, ?, ?>) o;
        return Objects.equals(_1, tuple3._1) && Objects.equals(_2, tuple3._2) && Objects.equals(_3, tuple3._3);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_1, _2, _3);
    }

    @Override
    public String toString() {
        return "Tuple3(" +
               "_1=" + _1 +
               ", _2=" + _2 +
               ", _3=" + _3 +
               ')';
    }
}
