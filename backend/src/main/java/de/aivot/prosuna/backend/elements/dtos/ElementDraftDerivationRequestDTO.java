package de.aivot.prosuna.backend.elements.dtos;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.ElementDerivationOptions;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

/**
 * Derives authored values against an element tree that is currently being edited.
 * <p>
 * When {@code element} is set, the unsaved draft is derived. Because its element functions are executed on the server,
 * endpoints must only accept a draft from users who may change the edited resource. Without a draft, the stored element
 * tree is derived instead.
 */
public record ElementDraftDerivationRequestDTO(
        @Nullable BaseElement element,
        @Nonnull @NotNull AuthoredElementValues authoredElementValues,
        @Nonnull @NotNull ElementDerivationOptions derivationOptions
) {
}
