package de.aivot.prosuna.backend.codeLists.filters;

import de.aivot.prosuna.backend.codeLists.entities.CodeListEntity;
import de.aivot.prosuna.backend.codeLists.enums.CodeListSourceType;
import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.utils.specification.SpecificationBuilder;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.domain.Specification;

import java.io.Serializable;

public class CodeListFilter implements Filter<CodeListEntity>, Serializable {
    private String key;
    private String name;
    private CodeListSourceType sourceType;
    private String sourceRef;

    public static CodeListFilter create() {
        return new CodeListFilter();
    }

    @NotNull
    @Override
    public Specification<CodeListEntity> build() {
        return SpecificationBuilder
                .create(CodeListEntity.class)
                .withContains("key", key)
                .withContains("name", name)
                .withEquals("sourceType", sourceType)
                .withEquals("sourceRef", sourceRef)
                .build();
    }

    public String getKey() {
        return key;
    }

    public CodeListFilter setKey(String key) {
        this.key = key;
        return this;
    }

    public String getName() {
        return name;
    }

    public CodeListFilter setName(String name) {
        this.name = name;
        return this;
    }

    public CodeListSourceType getSourceType() {
        return sourceType;
    }

    public CodeListFilter setSourceType(CodeListSourceType sourceType) {
        this.sourceType = sourceType;
        return this;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public CodeListFilter setSourceRef(String sourceRef) {
        this.sourceRef = sourceRef;
        return this;
    }
}
