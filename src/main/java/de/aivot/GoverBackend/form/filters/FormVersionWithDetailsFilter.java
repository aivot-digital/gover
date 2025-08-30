package de.aivot.GoverBackend.form.filters;

import de.aivot.GoverBackend.form.entities.FormEntity;
import de.aivot.GoverBackend.form.entities.FormVersionWithDetailsEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import org.springframework.data.jpa.domain.Specification;

public class FormVersionWithDetailsFilter implements Filter<FormVersionWithDetailsEntity> {
    @Override
    public Specification<FormVersionWithDetailsEntity> build() {
        return null;
    }
}
