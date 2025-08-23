package de.aivot.GoverBackend.dataObject.controllers;

import de.aivot.GoverBackend.dataObject.dtos.DataObjectItemRequestDTO;
import de.aivot.GoverBackend.dataObject.dtos.DataObjectItemResponseDTO;
import de.aivot.GoverBackend.dataObject.entities.DataObjectItemEntity;
import de.aivot.GoverBackend.dataObject.entities.DataObjectItemEntityId;
import de.aivot.GoverBackend.dataObject.filters.DataObjectItemFilter;
import de.aivot.GoverBackend.dataObject.services.DataObjectItemService;
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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/data-objects/{schemaKey}/items/")
public class DataObjectItemController {
    private final DataObjectItemService service;
    private final DataObjectSchemaService schemaService;

    @Autowired
    public DataObjectItemController(
            DataObjectItemService service,
            DataObjectSchemaService schemaService
    ) {
        this.service = service;
        this.schemaService = schemaService;
    }

    @GetMapping("")
    public Page<DataObjectItemResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid DataObjectItemFilter filter,
            @Nonnull @PathVariable String schemaKey
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        filter.setSchemaKey(schemaKey);

        var schema = schemaService
                .retrieve(schemaKey)
                .orElseThrow(ResponseException::notFound);

        return service
                .list(pageable, filter)
                .map(i -> DataObjectItemResponseDTO.fromEntity(i, schema));
    }

    @PostMapping("")
    public DataObjectItemResponseDTO create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody DataObjectItemRequestDTO requestDTO,
            @Nonnull @PathVariable String schemaKey
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        var schema = schemaService
                .retrieve(schemaKey)
                .orElseThrow(ResponseException::notFound);

        var entity = requestDTO
                .toEntity(schema);

        var created = service
                .create(entity);

        return DataObjectItemResponseDTO
                .fromEntity(created, schema);
    }

    @GetMapping("{itemId}/")
    public DataObjectItemResponseDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String schemaKey,
            @Nonnull @PathVariable String itemId
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var schema = schemaService
                .retrieve(schemaKey)
                .orElseThrow(ResponseException::notFound);

        var id = new DataObjectItemEntityId(schemaKey, itemId);

        return service
                .retrieve(id)
                .map(i -> DataObjectItemResponseDTO.fromEntity(i, schema))
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{itemId}/")
    public DataObjectItemResponseDTO update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String schemaKey,
            @Nonnull @PathVariable String itemId,
            @Nonnull @Valid @RequestBody DataObjectItemRequestDTO requestDTO
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        var schema = schemaService
                .retrieve(schemaKey)
                .orElseThrow(ResponseException::notFound);

        var id = new DataObjectItemEntityId(schemaKey, itemId);

        var entity = requestDTO
                .toEntity(schema);

        var updated = service
                .update(id, entity);

        return DataObjectItemResponseDTO
                .fromEntity(updated, schema);
    }

    @DeleteMapping("{itemId}/")
    public void destroy(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String schemaKey,
            @Nonnull @PathVariable String itemId
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        var id = new DataObjectItemEntityId(schemaKey, itemId);

        service.delete(id);
    }
}
