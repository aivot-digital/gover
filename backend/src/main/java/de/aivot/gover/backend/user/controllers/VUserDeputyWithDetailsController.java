package de.aivot.gover.backend.user.controllers;

import de.aivot.gover.backend.core.GenericReadController;
import de.aivot.gover.backend.openApi.OpenApiConfiguration;
import de.aivot.gover.backend.openApi.OpenApiConstants;
import de.aivot.gover.backend.user.entities.VUserDeputyWithDetailsEntity;
import de.aivot.gover.backend.user.filters.VUserDeputyWithDetailsFilter;
import de.aivot.gover.backend.user.services.UserService;
import de.aivot.gover.backend.user.services.VUserDeputyWithDetailsService;
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
