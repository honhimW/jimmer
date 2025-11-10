package org.babyfish.jimmer.sql.ast.tuple;

import org.babyfish.jimmer.sql.ast.impl.TupleImplementor;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * @author honhimW
 * @since 2025-11-10
 */

public class Tuple4<T1, T2, T3, T4> implements TupleImplementor {

    private final T1 _1;
    private final T2 _2;
    private final T3 _3;
    private final T4 _4;

    public Tuple4(T1 _1, T2 _2, T3 t3, T4 t4) {
        this._1 = _1;
        this._2 = _2;
        this._3 = t3;
        this._4 = t4;
    }

    @Override
    public int size() {
        return 4;
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
            case 3:
                return _4;
            default:
                throw new IllegalArgumentException(String.format("Index must between 0 and %d", size() - 1));
        }
    }

    @Override
    public TupleImplementor convert(BiFunction<Object, Integer, Object> block) {
        return new Tuple4<>(
            block.apply(_1, 0),
            block.apply(_2, 1),
            block.apply(_3, 2),
            block.apply(_4, 3)
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

    public T4 get_4() {
        return _4;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Tuple4<?, ?, ?, ?> tuple4 = (Tuple4<?, ?, ?, ?>) o;
        return Objects.equals(_1, tuple4._1) && Objects.equals(_2, tuple4._2) && Objects.equals(_3, tuple4._3) && Objects.equals(_4, tuple4._4);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_1, _2, _3, _4);
    }

    @Override
    public String toString() {
        return "Tuple4(" +
               "_1=" + _1 +
               ", _2=" + _2 +
               ", _3=" + _3 +
               ", _4=" + _4 +
               ')';
    }
}
