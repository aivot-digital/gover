package de.aivot.GoverBackend.form.services;

import de.aivot.GoverBackend.asset.repositories.AssetRepository;
import de.aivot.GoverBackend.department.repositories.DepartmentRepository;
import de.aivot.GoverBackend.destination.repositories.DestinationRepository;
import de.aivot.GoverBackend.elements.services.ElementApprovalService;
import de.aivot.GoverBackend.enums.SubmissionStatus;
import de.aivot.GoverBackend.form.entities.FormVersionEntity;
import de.aivot.GoverBackend.form.entities.FormVersionEntityId;
import de.aivot.GoverBackend.form.models.FormPublishChecklistItem;
import de.aivot.GoverBackend.form.repositories.FormRepository;
import de.aivot.GoverBackend.form.repositories.FormVersionRepository;
import de.aivot.GoverBackend.identity.entities.IdentityProviderEntity;
import de.aivot.GoverBackend.identity.models.IdentityProviderLink;
import de.aivot.GoverBackend.identity.repositories.IdentityProviderRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.payment.entities.PaymentProviderEntity;
import de.aivot.GoverBackend.payment.repositories.PaymentProviderRepository;
import de.aivot.GoverBackend.submission.entities.Submission;
import de.aivot.GoverBackend.submission.repositories.SubmissionRepository;
import de.aivot.GoverBackend.theme.repositories.ThemeRepository;
import de.aivot.GoverBackend.utils.StringUtils;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

@Service
public class FormVersionService implements EntityService<FormVersionEntity, FormVersionEntityId> {
    private final FormVersionRepository repository;
    private final DestinationRepository destinationRepository;
    private final DepartmentRepository departmentRepository;
    private final ThemeRepository themeRepository;
    private final AssetRepository assetRepository;
    private final PaymentProviderRepository paymentProviderRepository;
    private final IdentityProviderRepository identityProviderRepository;
    private final FormRepository formRepository;
    private final SubmissionRepository submissionRepository;

    @Autowired
    public FormVersionService(FormVersionRepository repository,
                              DestinationRepository destinationRepository,
                              DepartmentRepository departmentRepository,
                              ThemeRepository themeRepository,
                              AssetRepository assetRepository,
                              PaymentProviderRepository paymentProviderRepository,
                              IdentityProviderRepository identityProviderRepository,
                              FormRepository formRepository,
                              SubmissionRepository submissionRepository) {
        this.repository = repository;
        this.destinationRepository = destinationRepository;
        this.departmentRepository = departmentRepository;
        this.themeRepository = themeRepository;
        this.assetRepository = assetRepository;
        this.paymentProviderRepository = paymentProviderRepository;
        this.identityProviderRepository = identityProviderRepository;
        this.formRepository = formRepository;
        this.submissionRepository = submissionRepository;
    }

    @Nonnull
    @Override
    public Page<FormVersionEntity> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<FormVersionEntity> specification,
            @Nullable Filter<FormVersionEntity> filter
    ) {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public FormVersionEntity create(@Nonnull FormVersionEntity entity) throws ResponseException {
        var formId = entity.getFormId();

        if (formId == null) {
            throw ResponseException.badRequest("Die Formular-ID muss angegeben werden");
        }

        var form = formRepository
                .findById(formId)
                .orElse(null);
        if (form == null) {
            throw ResponseException.badRequest("Die angegebene Formular-ID existiert nicht");
        }

        var currentMaxVersion = repository
                .maxVersionForFormId(form.getId())
                .orElse(0);

        var cleanedEntity = cleanRelatedData(null, entity);

        cleanedEntity.setVersion(currentMaxVersion + 1);

        return repository.save(cleanedEntity);
    }

    public Optional<FormVersionEntity> retrieve(@Nonnull Integer formId, @Nonnull Integer formVersion) {
        var id = new FormVersionEntityId(formId, formVersion);
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<FormVersionEntity> retrieve(@Nonnull FormVersionEntityId id) {
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<FormVersionEntity> retrieve(@Nonnull Specification<FormVersionEntity> specification) {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull FormVersionEntityId id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<FormVersionEntity> specification) {
        return repository.exists(specification);
    }

    @Nonnull
    @Override
    public FormVersionEntity performUpdate(@Nonnull FormVersionEntityId id,
                                           @Nonnull FormVersionEntity entity,
                                           @Nonnull FormVersionEntity existingEntity) throws ResponseException {
        var cleanedEntity = cleanRelatedData(existingEntity, entity);

        var updatedExistingEntity = existingEntity
                .setType(cleanedEntity.getType())
                .setLegalSupportDepartmentId(cleanedEntity.getLegalSupportDepartmentId())
                .setTechnicalSupportDepartmentId(cleanedEntity.getTechnicalSupportDepartmentId())
                .setImprintDepartmentId(cleanedEntity.getImprintDepartmentId())
                .setPrivacyDepartmentId(cleanedEntity.getPrivacyDepartmentId())
                .setAccessibilityDepartmentId(cleanedEntity.getAccessibilityDepartmentId())
                .setCustomerAccessHours(cleanedEntity.getCustomerAccessHours())
                .setSubmissionRetentionWeeks(cleanedEntity.getSubmissionRetentionWeeks())
                .setThemeId(cleanedEntity.getThemeId())
                .setPdfTemplateKey(cleanedEntity.getPdfTemplateKey())
                .setPaymentProviderKey(cleanedEntity.getPaymentProviderKey())
                .setPaymentPurpose(cleanedEntity.getPaymentPurpose())
                .setPaymentDescription(cleanedEntity.getPaymentDescription())
                .setPaymentProducts(cleanedEntity.getPaymentProducts())
                .setIdentityVerificationRequired(cleanedEntity.getIdentityVerificationRequired())
                .setIdentityProviders(cleanedEntity.getIdentityProviders())
                .setDestinationId(cleanedEntity.getDestinationId())
                .setRootElement(cleanedEntity.getRootElement());

        return repository.save(updatedExistingEntity);
    }

    @Override
    public void performDelete(@Nonnull FormVersionEntity entity) throws ResponseException {
        if (entity.getPublished() != null && entity.getRevoked() == null) {
            throw ResponseException.conflict("Veröffentlichte Formularversionen können nicht gelöscht werden");
        }

        var submissionSpec = SpecificationBuilder
                .create(Submission.class)
                .withEquals("formId", entity.getFormId())
                .withEquals("version", entity.getVersion())
                .withNotEquals("status", SubmissionStatus.Archived)
                .withEquals("isTestSubmission", false)
                .build();

        var hasOpenSubmissions = submissionRepository
                .exists(submissionSpec);

        if (hasOpenSubmissions) {
            throw ResponseException.conflict("Die Formularversion kann nicht gelöscht werden, da noch offene Anträge vorhanden sind");
        }

        repository.delete(entity);
    }

    private FormVersionEntity cleanRelatedData(@Nullable FormVersionEntity prev, @Nonnull FormVersionEntity updated) throws ResponseException {
        checkAndReset(prev, updated, destinationRepository, FormVersionEntity::getDestinationId, updated::setDestinationId);

        checkAndReset(prev, updated, departmentRepository, FormVersionEntity::getLegalSupportDepartmentId, updated::setLegalSupportDepartmentId);
        checkAndReset(prev, updated, departmentRepository, FormVersionEntity::getTechnicalSupportDepartmentId, updated::setTechnicalSupportDepartmentId);

        checkAndReset(prev, updated, departmentRepository, FormVersionEntity::getImprintDepartmentId, updated::setImprintDepartmentId);
        checkAndReset(prev, updated, departmentRepository, FormVersionEntity::getPrivacyDepartmentId, updated::setPrivacyDepartmentId);
        checkAndReset(prev, updated, departmentRepository, FormVersionEntity::getAccessibilityDepartmentId, updated::setAccessibilityDepartmentId);

        checkAndReset(prev, updated, themeRepository, FormVersionEntity::getThemeId, updated::setThemeId);

        checkAndReset(prev, updated, assetRepository, FormVersionEntity::getPdfTemplateKey, updated::setPdfTemplateKey);

        checkAndReset(prev, updated, paymentProviderRepository, FormVersionEntity::getPaymentProviderKey, updated::setPaymentProviderKey);
        if (updated.getPaymentProviderKey() != null) {
            var paymentProvider = paymentProviderRepository
                    .findById(updated.getPaymentProviderKey())
                    .orElseThrow(ResponseException::internalServerError);

            if (!paymentProvider.getIsEnabled()) {
                updated.setPaymentProviderKey(null);
            }
        }

        // Remove all non-existing identity providers from the list of linked identity providers
        if (updated.getIdentityProviders() != null) {
            var cleanedIdentityProvider = new LinkedList<IdentityProviderLink>();
            for (var link : updated.getIdentityProviders()) {
                if (link.getIdentityProviderKey() != null && identityProviderRepository.existsById(link.getIdentityProviderKey())) {
                    cleanedIdentityProvider.add(link);
                }
            }
            updated.setIdentityProviders(cleanedIdentityProvider);
        }

        // Check if an identity is required but the list ist empty. Reset the requirement if this is the case
        if (updated.getIdentityProviders() == null || updated.getIdentityProviders().isEmpty()) {
            updated.setIdentityVerificationRequired(false);
        }

        return updated;
    }


    private static <T, I> void checkAndReset(@Nullable FormVersionEntity prev,
                                             @Nonnull FormVersionEntity updated,
                                             @Nonnull CrudRepository<T, I> repo,
                                             @Nonnull Function<FormVersionEntity, I> getter,
                                             @Nonnull Consumer<I> setter
    ) {
        var updatedValue = getter.apply(updated);

        // If the updated value is null, do nothing
        if (updatedValue == null) {
            return;
        }

        var previousValue = prev == null ? null : getter.apply(prev);

        // If both values are null or equal, do nothing
        if (Objects.equals(previousValue, updatedValue)) {
            return;
        }

        // If the updated value exists, do nothing
        if (repo.existsById(updatedValue)) {
            return;
        }

        // Otherwise reset the value to null
        setter.accept(null);
    }
}
