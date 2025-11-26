package de.aivot.GoverBackend.department.controllers;

import de.aivot.GoverBackend.department.entities.VDepartmentMembershipWithDetailsEntity;
import de.aivot.GoverBackend.department.filters.VDepartmentMembershipWithDetailsFilter;
import de.aivot.GoverBackend.department.services.VDepartmentMembershipWithDetailsService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.user.services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@RestController
@RequestMapping("/api/department-memberships-with-details/")
public class VDepartmentMembershipWithDetailsController {
    private final VDepartmentMembershipWithDetailsService service;

    @Autowired
    public VDepartmentMembershipWithDetailsController(VDepartmentMembershipWithDetailsService service) {
        this.service = service;
    }

    @GetMapping("")
    public Page<VDepartmentMembershipWithDetailsEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid VDepartmentMembershipWithDetailsFilter filter
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return service.list(pageable, filter);
    }

    @GetMapping("{id}/")
    public VDepartmentMembershipWithDetailsEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return service
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }
}

