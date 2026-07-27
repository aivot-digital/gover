package de.aivot.gover.backend.codeLists.filters;

import de.aivot.gover.backend.codeLists.entities.CodeListEntity;
import de.aivot.gover.backend.codeLists.enums.CodeListSourceType;
import de.aivot.gover.backend.lib.models.Filter;
import de.aivot.gover.backend.utils.specification.SpecificationBuilder;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.domain.Specification;

import java.io.Serializable;

public class CodeListFilter implements Filter<CodeListEntity>, Serializable {
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
                .withContains("name", name)
                .withEquals("sourceType", sourceType)
                .withEquals("sourceRef", sourceRef)
                .build();
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
