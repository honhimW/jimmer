package org.babyfish.jimmer.sql.model.inheritance;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.babyfish.jimmer.jackson.JsonConverter;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.model.hr.ConverterForIssue937;

import java.time.LocalDateTime;

@MappedSuperclass
public interface NamedEntity {

    @Key
    @JsonConverter(ConverterForIssue937.class)
    String getName();

    @LogicalDeleted(value = "true")
    boolean getDeleted();

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime getCreatedTime();

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime getModifiedTime();

    @Trait
    default String onNamedEntity() {
        return "onNamedEntity";
    }

    @Trait
    default String toBeOverride() {
        return "toBeOverride";
    }
}
