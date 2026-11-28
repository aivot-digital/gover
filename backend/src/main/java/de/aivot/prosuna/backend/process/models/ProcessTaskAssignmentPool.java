package de.aivot.prosuna.backend.process.models;

import de.aivot.prosuna.backend.elements.models.elements.form.input.AssignmentContextInputElementValue;
import de.aivot.prosuna.backend.elements.models.elements.form.input.DomainAndUserSelectInputElementValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * The Personenkreis a process element evaluated when it assigned a task: the selected teams, organizational units and
 * users, and the permissions the element requires. Memberships and rights are checked again on every reassignment.
 */
public record ProcessTaskAssignmentPool(
        @Nonnull List<DomainAndUserSelectInputElementValue> selection,
        @Nonnull List<String> requiredPermissions
) {
    @Nullable
    public static ProcessTaskAssignmentPool of(@Nullable AssignmentContextInputElementValue assignmentContext,
                                               @Nonnull List<String> requiredPermissions) {
        var selection = assignmentContext != null ? assignmentContext.getDomainAndUserSelection() : null;
        if (selection == null || selection.isEmpty()) {
            return null;
        }
        return new ProcessTaskAssignmentPool(
                selection.stream().filter(Objects::nonNull).toList(),
                List.copyOf(requiredPermissions)
        );
    }
}
