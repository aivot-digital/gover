package de.aivot.GoverBackend.form.dtos;

import de.aivot.GoverBackend.core.converters.RootElementConverter;
import de.aivot.GoverBackend.elements.models.elements.RootElement;
import de.aivot.GoverBackend.form.entities.FormEntity;
import de.aivot.GoverBackend.form.entities.FormVersionEntity;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.enums.FormType;
import de.aivot.GoverBackend.identity.models.IdentityProviderLink;
import de.aivot.GoverBackend.models.payment.PaymentProduct;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Convert;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record FormRequestDTO(
        @Nonnull
        @NotNull(message = "Die Slug darf nicht null sein")
        @NotBlank(message = "Die Slug darf nicht leer sein")
        @Size(min = 1, max = 255, message = "Die Slug muss mindestens 1 Zeichen lang sein, maximal aber 255 Zeichen lang sein")
        String slug,

        @Nonnull
        @NotNull(message = "Der interne Titel darf nicht null sein")
        @NotBlank(message = "Der interne Titel darf nicht leer sein")
        @Size(min = 1, max = 96, message = "Der Titel muss mindestens 1 Zeichen lang sein, maximal aber 96 Zeichen lang sein")
        String internalTitle,

        @Nonnull
        @NotNull(message = "Der öffentliche Titel darf nicht null sein")
        @NotBlank(message = "Der öffentliche Titel darf nicht leer sein")
        @Size(min = 1, max = 96, message = "title must be at least 1 character long")
        String publicTitle,

        @Nonnull
        @NotNull(message = "Die ID des entwickelnden Fachbereichs darf nicht null sein")
        Integer developingDepartmentId,

        @Nullable
        Integer managingDepartmentId,

        @Nullable
        Integer responsibleDepartmentId,

        @Nonnull
        @NotNull(message = "Der Typ darf nicht null sein")
        FormType type,

        @Nullable
        Integer legalSupportDepartmentId,

        @Nullable
        Integer technicalSupportDepartmentId,

        @Nullable
        Integer imprintDepartmentId,

        @Nullable
        Integer privacyDepartmentId,

        @Nullable
        Integer accessibilityDepartmentId,

        @Nullable
        Integer destinationId,

        @Nullable
        Integer themeId,

        @Nullable
        UUID pdfTemplateKey,

        @Nullable
        UUID paymentProviderKey,

        @Nullable
        @Size(max = 27, message = "Der Zahlungszweck darf maximal 27 Zeichen lang sein")
        String paymentPurpose,

        @Nullable
        @Size(max = 250, message = "Die Zahlungsbeschreibung darf maximal 250 Zeichen lang sein")
        String paymentDescription,

        @Nullable
        List<PaymentProduct> paymentProducts,

        @Nonnull
        @NotNull(message = "Die Anforderung zur Identitätsprüfung darf nicht null sein")
        Boolean identityVerificationRequired,

        @Nonnull
        @NotNull(message = "Die Liste der Identitätsanbieter darf nicht null sein, kann aber leer sein")
        List<IdentityProviderLink> identityProviders,

        @Nullable
        Integer customerAccessHours,

        @Nullable
        Integer submissionRetentionWeeks,

        @NotNull(message = "Das Root-Element darf nicht null sein")
        @Convert(converter = RootElementConverter.class)
        RootElement rootElement
) {
    @Nonnull
    public FormEntity toFormEntity() {
        // Do not fill in the auto generated data, for they are not being updated or inserted
        return new FormEntity(
                null,
                slug,
                internalTitle,
                developingDepartmentId,
                managingDepartmentId,
                responsibleDepartmentId,
                null,
                null,
                null,
                null,
                null
        );
    }

    public FormVersionEntity toFormVersionEntity() {
        return new FormVersionEntity(
                null,
                null,
                FormStatus.Drafted,
                publicTitle,
                type,
                legalSupportDepartmentId,
                technicalSupportDepartmentId,
                imprintDepartmentId,
                privacyDepartmentId,
                accessibilityDepartmentId,
                destinationId,
                customerAccessHours,
                submissionRetentionWeeks,
                themeId,
                pdfTemplateKey,
                paymentProviderKey,
                paymentPurpose,
                paymentDescription,
                paymentProducts,
                identityProviders,
                identityVerificationRequired,
                rootElement,
                null,
                null,
                null,
                null
        );
    }
}
