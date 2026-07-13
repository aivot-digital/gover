package de.aivot.gover.backend.codeLists.filters;

import de.aivot.gover.backend.codeLists.entities.CodeListEntity;
import de.aivot.gover.backend.lib.models.Filter;
import de.aivot.gover.backend.utils.specification.SpecificationBuilder;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.domain.Specification;

import java.io.Serializable;

public class CodeListFilter implements Filter<CodeListEntity>, Serializable {
    private String name;

    @NotNull
    @Override
    public Specification<CodeListEntity> build() {
        return SpecificationBuilder
                .create(CodeListEntity.class)
                .withContains("name", name)
                .build();
    }

    public String getName() {
        return name;
    }

    public CodeListFilter setName(String name) {
        this.name = name;
        return this;
    }
}
