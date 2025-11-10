package org.babyfish.jimmer.sql.ast.mutation;

/**
 * @author honhimW
 * @since 2025-11-10
 */

public interface MutationResultItem<E> {
    
    E getOriginalEntity();
    
    E getModifiedEntity();

    default boolean isModified() {
        return getOriginalEntity() != getModifiedEntity();
    }
    
}
