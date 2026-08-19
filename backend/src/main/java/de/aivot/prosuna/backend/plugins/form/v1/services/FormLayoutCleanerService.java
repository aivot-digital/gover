package de.aivot.prosuna.backend.plugins.form.v1.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.prosuna.backend.core.services.ObjectMapperFactory;
import de.aivot.prosuna.backend.elements.models.elements.form.content.ImageContentElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.prosuna.backend.elements.utils.ElementStreamUtils;

public class FormLayoutCleanerService {
    public static FormLayoutElement clean(FormLayoutElement formLayoutElement) {
        if (formLayoutElement == null) {
            return null;
        }

        var om = ObjectMapperFactory
                .getInstance();

        String json;
        try {
            json = om.writeValueAsString(formLayoutElement);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        FormLayoutElement copy;
        try {
            copy = om.readValue(json, FormLayoutElement.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        copy.setThemeId(null);
        copy.setManagingDepartmentId(null);
        copy.setResponsibleDepartmentId(null);
        copy.setAccessibilityDepartmentId(null);
        copy.setImprintDepartmentId(null);
        copy.setPrivacyDepartmentId(null);
        copy.setPdfTemplateKey(null);
        copy.setLegalSupportDepartmentId(null);
        copy.setTechnicalSupportDepartmentId(null);

        ElementStreamUtils.applyAction(copy, element -> {
            switch (element) {
                case ImageContentElement ice:
                    ice.setSrc(null);
                    break;
                default:
                    break;
            }
        });

        return copy;
    }
}
