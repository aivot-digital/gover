package de.aivot.GoverBackend.dataObject.controllers;

import de.aivot.GoverBackend.dataObject.entities.DataObjectSchemaEntity;
import de.aivot.GoverBackend.dataObject.filters.DataObjectSchemaFilter;
import de.aivot.GoverBackend.dataObject.services.DataObjectSchemaService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.user.services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@RestController
@RequestMapping("/api/data-objects/")
public class DataObjectSchemaController {
    private final DataObjectSchemaService service;

    @Autowired
    public DataObjectSchemaController(
            DataObjectSchemaService service
    ) {
        this.service = service;
    }

    @GetMapping("")
    public Page<DataObjectSchemaEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid DataObjectSchemaFilter filter
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return service
                .list(pageable, filter);
    }

    @PostMapping("")
    public DataObjectSchemaEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody DataObjectSchemaEntity newDataObjectEntity
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        return service.create(newDataObjectEntity);
    }

    @GetMapping("{key}/")
    public DataObjectSchemaEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String key
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return service
                .retrieve(key)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{key}/")
    public DataObjectSchemaEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String key,
            @Nonnull @Valid @RequestBody DataObjectSchemaEntity updatedDataObjectEntity
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        return service
                .update(key, updatedDataObjectEntity);
    }

    @DeleteMapping("{key}/")
    public void destroy(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String key
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        service.delete(key);
    }
}
