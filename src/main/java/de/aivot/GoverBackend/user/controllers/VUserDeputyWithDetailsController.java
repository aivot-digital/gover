package de.aivot.GoverBackend.user.controllers;

import de.aivot.GoverBackend.core.GenericReadController;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.user.entities.VUserDeputyWithDetailsEntity;
import de.aivot.GoverBackend.user.filters.VUserDeputyWithDetailsFilter;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.user.services.VUserDeputyWithDetailsService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/user-deputies-with-details/")
@Tag(
        name = OpenApiConstants.Tags.UserDeputiesName,
        description = OpenApiConstants.Tags.UserDeputiesDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class VUserDeputyWithDetailsController extends GenericReadController<VUserDeputyWithDetailsEntity, Integer, VUserDeputyWithDetailsFilter> {
    public VUserDeputyWithDetailsController(UserService userService,
                                            VUserDeputyWithDetailsService service) {
        super(userService, service);
    }

    // TODO: Implement Permission Checks
}
