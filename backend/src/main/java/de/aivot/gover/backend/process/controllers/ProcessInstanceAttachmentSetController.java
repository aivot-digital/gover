package de.aivot.gover.backend.process.controllers;

import de.aivot.gover.backend.core.GenericReadController;
import de.aivot.gover.backend.openApi.OpenApiConfiguration;
import de.aivot.gover.backend.openApi.OpenApiConstants;
import de.aivot.gover.backend.process.entities.ProcessInstanceAttachmentSetEntity;
import de.aivot.gover.backend.process.filters.ProcessInstanceAttachmentSetFilter;
import de.aivot.gover.backend.process.services.ProcessInstanceAttachmentSetService;
import de.aivot.gover.backend.user.services.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/process-instance-attachment-sets/")
@Tag(
        name = OpenApiConstants.Tags.ProcessesDefinitionsName,
        description = "Operations for reading process instance attachment sets."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProcessInstanceAttachmentSetController extends GenericReadController<ProcessInstanceAttachmentSetEntity, Integer, ProcessInstanceAttachmentSetFilter> {
    public ProcessInstanceAttachmentSetController(UserService userService,
                                                  ProcessInstanceAttachmentSetService processInstanceAttachmentSetService) {
        super(userService, processInstanceAttachmentSetService);
    }
}
