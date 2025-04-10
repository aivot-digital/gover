package de.aivot.GoverBackend.nocode.controllers;

import de.aivot.GoverBackend.elements.models.ElementDerivationData;
import de.aivot.GoverBackend.nocode.dtos.NoCodeEvaluationRequestDTO;
import de.aivot.GoverBackend.nocode.dtos.NoCodeEvaluationResponseDTO;
import de.aivot.GoverBackend.nocode.models.NoCodeExpression;
import de.aivot.GoverBackend.nocode.services.NoCodeEvaluationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NoCodeEvaluationController {
    private final NoCodeEvaluationService noCodeEvaluationService;

    @Autowired
    public NoCodeEvaluationController(NoCodeEvaluationService noCodeEvaluationService) {
        this.noCodeEvaluationService = noCodeEvaluationService;
    }

    @PostMapping("/api/no-code/evaluate")
    public NoCodeEvaluationResponseDTO evaluate(
            @RequestBody NoCodeEvaluationRequestDTO request
    ) {
        var expression = new NoCodeExpression(request.expression());
        var data = new ElementDerivationData(request.data());
        var result = noCodeEvaluationService.evaluate(expression, data, null);
        return new NoCodeEvaluationResponseDTO(result.getValue());
    }
}
