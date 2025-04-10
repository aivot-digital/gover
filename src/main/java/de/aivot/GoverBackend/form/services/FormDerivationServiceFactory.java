package de.aivot.GoverBackend.form.services;


import de.aivot.GoverBackend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.GoverBackend.form.entities.Form;
import de.aivot.GoverBackend.nocode.services.NoCodeEvaluationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@Service
public class FormDerivationServiceFactory {
    private final JavascriptEngineFactoryService javascriptEngineFactoryService;
    private final NoCodeEvaluationService noCodeEvaluationService;

    @Autowired
    public FormDerivationServiceFactory(JavascriptEngineFactoryService javascriptEngineFactoryService, NoCodeEvaluationService noCodeEvaluationService) {
        this.javascriptEngineFactoryService = javascriptEngineFactoryService;
        this.noCodeEvaluationService = noCodeEvaluationService;
    }

    public FormDerivationService create(
            @Nonnull Form form,
            @Nonnull List<String> stepsToValidate,
            @Nonnull List<String> stepsToCalculateVisibilities,
            @Nonnull List<String> stepsToCalculateValues,
            @Nonnull List<String> stepsToCalculateOverrides
    ) {
        return new FormDerivationService(
                form,
                stepsToValidate,
                stepsToCalculateVisibilities,
                stepsToCalculateValues,
                stepsToCalculateOverrides,
                javascriptEngineFactoryService,
                noCodeEvaluationService
        );
    }
}
