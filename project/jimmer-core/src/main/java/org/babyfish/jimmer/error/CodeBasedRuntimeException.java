package org.babyfish.jimmer.error;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author honhimW
 * @since 2025-11-10
 */

public class CodeBasedRuntimeException extends RuntimeException {

    private final ClientExceptionMetadata metadata = ClientExceptionMetadata.of(this.getClass());

    private Map<String, Object> fields;

    protected CodeBasedRuntimeException(@Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
    }

    public CodeBasedRuntimeException() {
        this(null, null);
    }

    public CodeBasedRuntimeException(@Nullable String message) {
        this(message, null);
    }

    public CodeBasedRuntimeException(@Nullable Throwable cause) {
        this(null, cause);
    }

    public Map<String, Object> getFields() {
        if (fields == null) {
            fields = new HashMap<>();
            metadata.getGetterMap().forEach((k, v) -> {
                try {
                    fields.put(k, v.invoke(this));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return fields;
    }

    public String getFamily() {
        return metadata.getFamily();
    }
    public String getCode() {
        return metadata.getCode();
    }

}
