package de.aivot.GoverBackend.query.controllers;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.query.exceptions.QueryValidationException;
import de.aivot.GoverBackend.query.models.QuerySelect;
import de.aivot.GoverBackend.query.services.QueryEvaluationService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@RestController
@RequestMapping("/api/query-evaluation/")
public class QueryEvaluationController {
    private final QueryEvaluationService queryEvaluationService;

    public QueryEvaluationController(QueryEvaluationService queryEvaluationService) {
        this.queryEvaluationService = queryEvaluationService;
    }

    @PostMapping("sql/")
    public String sql(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody QuerySelect select
    ) throws ResponseException {
        try {
            select.validate();
        } catch (QueryValidationException e) {
            throw ResponseException.badRequest(e.getMessage(), e);
        }
        try {
            return select.build();
        } catch (QueryValidationException e) {
            throw ResponseException.badRequest(e.getMessage(), e);
        }
    }

    /*
    @PostMapping("evaluate/")
    public Object evaluate(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody QuerySelect select
    ) throws ResponseException {
        try {
            return queryEvaluationService
                    .evaluate(select);
        } catch (QueryValidationException e) {
            throw ResponseException.badRequest(e.getMessage(), e);
        }
    }
     */
}
