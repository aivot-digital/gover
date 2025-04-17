package de.aivot.GoverBackend.form.dtos;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import de.aivot.GoverBackend.core.converters.JacksonRootElementDeserializer;
import de.aivot.GoverBackend.core.converters.JacksonRootElementSerializer;
import de.aivot.GoverBackend.core.converters.RootElementConverter;
import de.aivot.GoverBackend.enums.*;
import de.aivot.GoverBackend.form.entities.Form;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.enums.FormType;
import de.aivot.GoverBackend.identity.models.IdentityProviderLink;
import de.aivot.GoverBackend.lib.ReqeustDTO;
import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.models.payment.PaymentProduct;
import jakarta.persistence.Convert;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

public record FormRequestDTO(
        @NotNull(message = "slug cannot be null")
        @NotBlank(message = "slug cannot be blank")
        @Length(min = 1, max = 255, message = "slug must be at least 1 character long")
        String slug,

        @NotNull(message = "version cannot be null")
        @NotBlank(message = "version cannot be blank")
        @Length(min = 5, max = 11, message = "version must be at least 5 character long")
        String version,

        @NotNull(message = "title cannot be null")
        @NotBlank(message = "title cannot be blank")
        @Length(min = 1, max = 96, message = "title must be at least 1 character long")
        String title,

        @NotNull(message = "status cannot be null")
        FormStatus status,

        FormType type, // Defaults to FormType.Public (0)

        @NotNull(message = "root cannot be null")
        @Convert(converter = RootElementConverter.class)
        @JsonSerialize(converter = JacksonRootElementSerializer.class)
        @JsonDeserialize(converter = JacksonRootElementDeserializer.class)
        RootElement root,

        @NotNull(message = "destinationId cannot be null")
        Integer developingDepartmentId,

        Integer destinationId,
        Integer legalSupportDepartmentId,
        Integer technicalSupportDepartmentId,
        Integer imprintDepartmentId,
        Integer privacyDepartmentId,
        Integer accessibilityDepartmentId,
        Integer managingDepartmentId,
        Integer responsibleDepartmentId,
        Integer themeId,

        Integer customerAccessHours,
        Integer submissionDeletionWeeks,

        @Length(max = 36, message = "uuid must be 36 characters long")
        String pdfBodyTemplateKey,

        Collection<PaymentProduct> products,

        @Length(max = 27, message = "paymentPurpose must be at most 27 characters long")
        String paymentPurpose,

        @Length(max = 250, message = "paymentDescription must be at most 250 characters long")
        String paymentDescription,

        @Length(max = 36, message = "uuid must be 36 characters long")
        String paymentProvider,

        @NotNull(message = "identityRequired cannot be null")
        Boolean identityRequired,

        @NotNull(message = "identityProviders cannot be null")
        List<IdentityProviderLink> identityProviders
) implements ReqeustDTO<Form> {
    @Nonnull
    @Override
    public Form toEntity() {
        var form = new Form();

        form.setSlug(slug);
        form.setVersion(version);
        form.setTitle(title);
        form.setStatus(status);
        form.setType(type != null ? type : FormType.Public);
        form.setRoot(root);
        form.setDevelopingDepartmentId(developingDepartmentId);
        form.setDestinationId(destinationId);
        form.setLegalSupportDepartmentId(legalSupportDepartmentId);
        form.setTechnicalSupportDepartmentId(technicalSupportDepartmentId);
        form.setImprintDepartmentId(imprintDepartmentId);
        form.setPrivacyDepartmentId(privacyDepartmentId);
        form.setAccessibilityDepartmentId(accessibilityDepartmentId);
        form.setManagingDepartmentId(managingDepartmentId);
        form.setResponsibleDepartmentId(responsibleDepartmentId);
        form.setThemeId(themeId);
        form.setCustomerAccessHours(customerAccessHours);
        form.setSubmissionDeletionWeeks(submissionDeletionWeeks);
        form.setPdfBodyTemplateKey(pdfBodyTemplateKey);
        form.setProducts(products);
        form.setPaymentPurpose(paymentPurpose);
        form.setPaymentDescription(paymentDescription);
        form.setPaymentProvider(paymentProvider);
        form.setIdentityRequired(identityRequired);
        form.setIdentityProviders(identityProviders);

        return form;
    }
}
