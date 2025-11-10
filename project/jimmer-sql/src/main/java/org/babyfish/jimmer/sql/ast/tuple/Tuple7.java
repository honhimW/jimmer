package org.babyfish.jimmer.sql.ast.tuple;

import org.babyfish.jimmer.sql.ast.impl.TupleImplementor;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * @author honhimW
 * @since 2025-11-10
 */

public class Tuple7<T1, T2, T3, T4, T5, T6, T7> implements TupleImplementor {

    private final T1 _1;
    private final T2 _2;
    private final T3 _3;
    private final T4 _4;
    private final T5 _5;
    private final T6 _6;
    private final T7 _7;

    public Tuple7(T1 _1, T2 _2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7) {
        this._1 = _1;
        this._2 = _2;
        this._3 = t3;
        this._4 = t4;
        this._5 = t5;
        this._6 = t6;
        this._7 = t7;
    }

    @Override
    public int size() {
        return 7;
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
            case 4:
                return _5;
            case 5:
                return _6;
            case 6:
                return _7;
            default:
                throw new IllegalArgumentException(String.format("Index must between 0 and %d", size() - 1));
        }
    }

    @Override
    public TupleImplementor convert(BiFunction<Object, Integer, Object> block) {
        return new Tuple7<>(
            block.apply(_1, 0),
            block.apply(_2, 1),
            block.apply(_3, 2),
            block.apply(_4, 3),
            block.apply(_5, 4),
            block.apply(_6, 5),
            block.apply(_7, 6)
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

    public T5 get_5() {
        return _5;
    }

    public T6 get_6() {
        return _6;
    }

    public T7 get_7() {
        return _7;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Tuple7<?, ?, ?, ?, ?, ?, ?> tuple7 = (Tuple7<?, ?, ?, ?, ?, ?, ?>) o;
        return Objects.equals(_1, tuple7._1) && Objects.equals(_2, tuple7._2) && Objects.equals(_3, tuple7._3) && Objects.equals(_4, tuple7._4) && Objects.equals(_5, tuple7._5) && Objects.equals(_6, tuple7._6) && Objects.equals(_7, tuple7._7);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_1, _2, _3, _4, _5, _6, _7);
    }

    @Override
    public String toString() {
        return "Tuple7{" +
               "_1=" + _1 +
               ", _2=" + _2 +
               ", _3=" + _3 +
               ", _4=" + _4 +
               ", _5=" + _5 +
               ", _6=" + _6 +
               ", _7=" + _7 +
               '}';
    }
}
