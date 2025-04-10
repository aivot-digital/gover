package de.aivot.GoverBackend.nocode.dtos;

import java.util.Map;

public record NoCodeEvaluationRequestDTO(Map<String, Object> expression, Map<String, Object> data) {
}
