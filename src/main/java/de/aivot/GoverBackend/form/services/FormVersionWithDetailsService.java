package de.aivot.GoverBackend.form.services;

import de.aivot.GoverBackend.elements.services.ElementApprovalService;
import de.aivot.GoverBackend.form.entities.FormVersionEntityId;
import de.aivot.GoverBackend.form.entities.FormVersionWithDetailsEntity;
import de.aivot.GoverBackend.form.entities.FormVersionWithDetailsEntityId;
import de.aivot.GoverBackend.form.models.FormPublishChecklistItem;
import de.aivot.GoverBackend.form.repositories.FormRepository;
import de.aivot.GoverBackend.form.repositories.FormVersionRepository;
import de.aivot.GoverBackend.form.repositories.FormVersionWithDetailsRepository;
import de.aivot.GoverBackend.identity.entities.IdentityProviderEntity;
import de.aivot.GoverBackend.identity.repositories.IdentityProviderRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.ReadEntityService;
import de.aivot.GoverBackend.payment.entities.PaymentProviderEntity;
import de.aivot.GoverBackend.payment.repositories.PaymentProviderRepository;
import de.aivot.GoverBackend.utils.StringUtils;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Service
public class FormVersionWithDetailsService implements ReadEntityService<FormVersionWithDetailsEntity, FormVersionWithDetailsEntityId> {
    private final FormVersionWithDetailsRepository repository;
    private final PaymentProviderRepository paymentProviderRepository;
    private final IdentityProviderRepository identityProviderRepository;
    private final FormVersionRepository formVersionRepository;
    private final FormRepository formRepository;

    @Autowired
    public FormVersionWithDetailsService(FormVersionWithDetailsRepository repository,
                                         PaymentProviderRepository paymentProviderRepository,
                                         IdentityProviderRepository identityProviderRepository,
                                         FormVersionRepository formVersionRepository,
                                         FormRepository formRepository) {
        this.repository = repository;
        this.paymentProviderRepository = paymentProviderRepository;
        this.identityProviderRepository = identityProviderRepository;
        this.formVersionRepository = formVersionRepository;
        this.formRepository = formRepository;
    }

    @Nonnull
    @Override
    public Page<FormVersionWithDetailsEntity> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<FormVersionWithDetailsEntity> specification,
            @Nullable Filter<FormVersionWithDetailsEntity> filter
    ) {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<FormVersionWithDetailsEntity> retrieve(@Nonnull FormVersionWithDetailsEntityId id) {
        return repository.findById(id);
    }

    @Nonnull
    public Optional<FormVersionWithDetailsEntity> retrieve(@Nonnull Integer formId, @Nonnull Integer formVersion) {
        return retrieve(FormVersionWithDetailsEntityId.of(formId, formVersion));
    }

    @Nonnull
    @Override
    public Optional<FormVersionWithDetailsEntity> retrieve(@Nonnull Specification<FormVersionWithDetailsEntity> specification) {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull FormVersionWithDetailsEntityId id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<FormVersionWithDetailsEntity> specification) {
        return repository.exists(specification);
    }

    @Nonnull
    public Page<FormVersionWithDetailsEntity> findAllByIsCurrentlyPublishedVersionIsTrue(@Nullable Pageable pageable, @Nullable Specification<FormVersionWithDetailsEntity> spec) {
        return repository.findAllByIsCurrentlyPublishedVersionIsTrue(pageable, spec);
    }

    @Nonnull
    public Optional<FormVersionWithDetailsEntity> findBySlugAndIsCurrentlyPublishedVersionIsTrue(@Nonnull String slug) {
        return repository.findBySlugAndIsCurrentlyPublishedVersionIsTrue(slug);
    }

    @Nonnull
    public Optional<FormVersionWithDetailsEntity> findBySlugAndVersion(@Nonnull String slug, @Nonnull Integer version) {
        return repository.findBySlugAndVersion(slug, version);
    }

    @Nonnull
    public FormVersionWithDetailsEntity publish(@Nonnull FormVersionWithDetailsEntity form) throws ResponseException {
        var allChecklistItemsDone = getFormPublishChecklist(form)
                .stream()
                .allMatch(FormPublishChecklistItem::getDone);

        if (!allChecklistItemsDone) {
            throw new ResponseException(HttpStatus.CONFLICT, "Das Formular kann nicht veröffentlicht werden, da nicht alle Voraussetzungen erfüllt sind");
        }

        var formToPublish = formRepository
                .findById(form.getId())
                .orElseThrow(ResponseException::notFound);

        var versionToPublishId = FormVersionEntityId
                .of(form.getId(), form.getVersion());

        var versionToPublish = formVersionRepository
                .findById(versionToPublishId)
                .orElseThrow(ResponseException::notFound);

        versionToPublish.setPublished(LocalDateTime.now());
        var publishedVersion = formVersionRepository.save(versionToPublish);

        formToPublish.setPublishedVersion(form.getVersion());
        var publishedForm = formRepository.save(formToPublish);

        return FormVersionWithDetailsEntity
                .of(publishedForm, publishedVersion);
    }

    @Nonnull
    public FormVersionWithDetailsEntity revoke(@Nonnull FormVersionWithDetailsEntity form) throws ResponseException {
        if (form.getPublished() == null) {
            throw new ResponseException(HttpStatus.CONFLICT, "Das Formular ist nicht veröffentlicht und kann nicht zurückgezogen werden");
        }

        var formToRevoke = formRepository
                .findById(form.getId())
                .orElseThrow(ResponseException::notFound);

        var versionToRevokeId = FormVersionEntityId
                .of(form.getId(), form.getVersion());

        var versionToRevoke = formVersionRepository
                .findById(versionToRevokeId)
                .orElseThrow(ResponseException::notFound);

        versionToRevoke.setRevoked(LocalDateTime.now());
        var revokedVersion = formVersionRepository.save(versionToRevoke);

        formToRevoke.setPublishedVersion(null);
        var revokedForm = formRepository.save(formToRevoke);


        return FormVersionWithDetailsEntity
                .of(revokedForm, revokedVersion);
    }

    public List<FormPublishChecklistItem> getFormPublishChecklist(FormVersionWithDetailsEntity form) {
        var checklist = new LinkedList<FormPublishChecklistItem>();

        checklist.add(
                FormPublishChecklistItem
                        .create()
                        .setLabel("Öffentlicher Titel & Überschrift hinterlegt")
                        .setDone(StringUtils.isNotNullOrEmpty(form.getPublicTitle()))
        );

        checklist.add(
                FormPublishChecklistItem
                        .create()
                        .setLabel("Fachlicher Support eingerichtet")
                        .setDone(form.getLegalSupportDepartmentId() != null)
        );

        checklist.add(
                FormPublishChecklistItem
                        .create()
                        .setLabel("Technischer Support eingerichtet")
                        .setDone(form.getTechnicalSupportDepartmentId() != null)
        );

        checklist.add(
                FormPublishChecklistItem
                        .create()
                        .setLabel("Impressum eingerichtet")
                        .setDone(form.getImprintDepartmentId() != null)
        );

        checklist.add(
                FormPublishChecklistItem
                        .create()
                        .setLabel("Datenschutzerklärung eingerichtet")
                        .setDone(form.getPrivacyDepartmentId() != null)
        );

        checklist.add(
                FormPublishChecklistItem
                        .create()
                        .setLabel("Barrierefreiheitserklärung eingerichtet")
                        .setDone(form.getAccessibilityDepartmentId() != null)
        );

        checklist.add(
                FormPublishChecklistItem
                        .create()
                        .setLabel("Löschfrist für Anträge festgelegt")
                        .setDone(form.getSubmissionRetentionWeeks() != null && form.getSubmissionRetentionWeeks() >= 0)
        );

        checklist.add(
                FormPublishChecklistItem
                        .create()
                        .setLabel("Zugriffszeit für Bürger:innen festgelegt")
                        .setDone(form.getCustomerAccessHours() != null && form.getCustomerAccessHours() >= 0)
        );

        if (form.getPaymentProviderKey() != null) {
            var paymentProviderSpec = SpecificationBuilder
                    .create(PaymentProviderEntity.class)
                    .withEquals("key", form.getPaymentProviderKey())
                    .withEquals("isTestProvider", false)
                    .build();

            checklist.add(
                    FormPublishChecklistItem
                            .create()
                            .setLabel("Nutzt ausschließlich einen produktiven Zahlungsdienstleister")
                            .setDone(paymentProviderRepository.exists(paymentProviderSpec))
            );
        }

        if (form.getIdentityProviders() != null && !form.getIdentityProviders().isEmpty()) {


            var allLinkedIDPsProductive = form
                    .getIdentityProviders()
                    .stream()
                    .allMatch(idp -> {
                        if (idp.getIdentityProviderKey() == null) {
                            return false;
                        }

                        var identityProviderSpec = SpecificationBuilder
                                .create(IdentityProviderEntity.class)
                                .withEquals("key", idp.getIdentityProviderKey())
                                .withEquals("isTestProvider", false)
                                .withEquals("isEnabled", true)
                                .build();

                        return identityProviderRepository.exists(identityProviderSpec);
                    });

            checklist.add(
                    FormPublishChecklistItem
                            .create()
                            .setLabel("Beinhaltet ausschließlich produktive Nutzerkontenanbieter")
                            .setDone(allLinkedIDPsProductive)
            );
        }

        if (form.getIdentityVerificationRequired() && (form.getIdentityProviders() == null || form.getIdentityProviders().isEmpty())) {
            checklist.add(
                    FormPublishChecklistItem
                            .create()
                            .setLabel("Beinhaltet benötigte Nutzerkontenanbieter")
                            .setDone(false)
            );
        }

        checklist.add(
                FormPublishChecklistItem
                        .create()
                        .setLabel("Alle Elemente des Formulars geprüft")
                        .setDone(ElementApprovalService.isApproved(form.getRootElement()))
        );

        return checklist;
    }
}
