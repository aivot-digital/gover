package de.aivot.GoverBackend.form.services;

import de.aivot.GoverBackend.asset.services.AssetService;
import de.aivot.GoverBackend.config.services.SystemConfigService;
import de.aivot.GoverBackend.department.services.DepartmentService;
import de.aivot.GoverBackend.destination.services.DestinationService;
import de.aivot.GoverBackend.elements.services.ElementApprovalService;
import de.aivot.GoverBackend.enums.SubmissionStatus;
import de.aivot.GoverBackend.form.entities.Form;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.models.FormPublishChecklistItem;
import de.aivot.GoverBackend.form.repositories.FormRepository;
import de.aivot.GoverBackend.identity.filters.IdentityProviderFilter;
import de.aivot.GoverBackend.identity.models.IdentityProviderLink;
import de.aivot.GoverBackend.identity.services.IdentityProviderService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.payment.services.PaymentProviderService;
import de.aivot.GoverBackend.submission.entities.Submission;
import de.aivot.GoverBackend.submission.repositories.SubmissionRepository;
import de.aivot.GoverBackend.submission.services.SubmissionService;
import de.aivot.GoverBackend.theme.services.ThemeService;
import de.aivot.GoverBackend.utils.StringUtils;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Service
public class FormService implements EntityService<Form, Integer> {
    private final FormRepository repository;

    private final DestinationService destinationService;
    private final DepartmentService departmentService;
    private final ThemeService themeService;
    private final PaymentProviderService paymentProviderService;
    private final AssetService assetService;
    private final SubmissionService submissionService;
    private final SubmissionRepository submissionRepository;
    private final IdentityProviderService identityProviderService;

    @Autowired
    public FormService(
            FormRepository repository,
            DestinationService destinationService,
            DepartmentService departmentService,
            ThemeService themeService,
            PaymentProviderService paymentProviderService,
            AssetService assetService,
            SubmissionService submissionService,
            SubmissionRepository submissionRepository,
            IdentityProviderService identityProviderService
    ) {
        this.repository = repository;
        this.destinationService = destinationService;
        this.departmentService = departmentService;
        this.themeService = themeService;
        this.paymentProviderService = paymentProviderService;
        this.assetService = assetService;
        this.submissionService = submissionService;
        this.submissionRepository = submissionRepository;
        this.identityProviderService = identityProviderService;
    }

    @Nonnull
    @Override
    public Page<Form> performList(@Nonnull Pageable pageable, @Nullable Specification<Form> specification, Filter<Form> filter) {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Form create(@Nonnull Form entity) throws ResponseException {
        entity.setId(null);
        entity.setStatus(FormStatus.Drafted);

        if (repository.existsBySlugAndVersion(entity.getSlug(), entity.getVersion())) {
            throw new ResponseException(HttpStatus.CONFLICT, "Es existiert bereits ein Formular mit dieser URL und dieser Version");
        }

        var cleanedEntity = cleanRelatedData(entity);

        return repository.save(cleanedEntity);
    }

    @Nonnull
    @Override
    public Form performUpdate(@Nonnull Integer id, @Nonnull Form updatedForm, @Nonnull Form existingForm) throws ResponseException {
        if (existingForm.getStatus() == FormStatus.Published) {
            throw new ResponseException(HttpStatus.CONFLICT, "Das Formular ist bereits veröffentlicht und kann nicht bearbeitet werden");
        }

        if (existingForm.getStatus() == FormStatus.Revoked) {
            throw new ResponseException(HttpStatus.CONFLICT, "Das Formular wurde bereits veröffentlicht und zurückgezogen und kann nicht bearbeitet werden");
        }

        existingForm.setTitle(updatedForm.getTitle());

        existingForm.setType(updatedForm.getType());

        existingForm.setRoot(updatedForm.getRoot());
        existingForm.setSubmissionDeletionWeeks(updatedForm.getSubmissionDeletionWeeks());
        existingForm.setCustomerAccessHours(updatedForm.getCustomerAccessHours());

        existingForm.setDestinationId(updatedForm.getDestinationId());

        existingForm.setLegalSupportDepartmentId(updatedForm.getLegalSupportDepartmentId());

        existingForm.setTechnicalSupportDepartmentId(updatedForm.getTechnicalSupportDepartmentId());

        existingForm.setImprintDepartmentId(updatedForm.getImprintDepartmentId());

        existingForm.setPrivacyDepartmentId(updatedForm.getPrivacyDepartmentId());

        existingForm.setAccessibilityDepartmentId(updatedForm.getAccessibilityDepartmentId());
        existingForm.setDevelopingDepartmentId(updatedForm.getDevelopingDepartmentId());
        existingForm.setManagingDepartmentId(updatedForm.getManagingDepartmentId());
        existingForm.setResponsibleDepartmentId(updatedForm.getResponsibleDepartmentId());
        existingForm.setThemeId(updatedForm.getThemeId());
        existingForm.setPdfBodyTemplateKey(updatedForm.getPdfBodyTemplateKey());

        existingForm.setProducts(updatedForm.getProducts());
        existingForm.setPaymentPurpose(updatedForm.getPaymentPurpose());
        existingForm.setPaymentDescription(updatedForm.getPaymentDescription());

        existingForm.setPaymentProvider(updatedForm.getPaymentProvider());

        existingForm.setIdentityRequired(updatedForm.getIdentityRequired());
        existingForm.setIdentityProviders(updatedForm.getIdentityProviders());

        cleanRelatedData(existingForm);

        return repository.save(existingForm);
    }

    @Nonnull
    @Override
    public Optional<Form> retrieve(@Nonnull Integer id) {
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<Form> retrieve(@Nonnull Specification<Form> specification) {
        return repository.findOne(specification);
    }

    public Optional<String> retrieveLatestPublishedVersionBySlug(@Nonnull String slug) {
        return repository.getLatestVersionBySlugAndStatus(slug, FormStatus.Published);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<Form> specification) {
        return repository.exists(specification);
    }

    private Form cleanRelatedData(Form form) throws ResponseException {
        // Remove the destination id if it does not exist
        if (form.getDestinationId() == null || !destinationService.exists(form.getDestinationId())) {
            form.setDestinationId(null);
        }

        // Remove the legal support department id if it does not exist
        if (form.getLegalSupportDepartmentId() == null || !departmentService.exists(form.getLegalSupportDepartmentId())) {
            form.setLegalSupportDepartmentId(null);
        }

        // Remove the technical support department id if it does not exist
        if (form.getTechnicalSupportDepartmentId() == null || !departmentService.exists(form.getTechnicalSupportDepartmentId())) {
            form.setTechnicalSupportDepartmentId(null);
        }

        // Remove the imprint department id if it does not exist
        if (form.getImprintDepartmentId() == null || !departmentService.exists(form.getImprintDepartmentId())) {
            form.setImprintDepartmentId(null);
        }

        // Remove the privacy department id if it does not exist
        if (form.getPrivacyDepartmentId() == null || !departmentService.exists(form.getPrivacyDepartmentId())) {
            form.setPrivacyDepartmentId(null);
        }

        // Remove the accessibility department id if it does not exist
        if (form.getAccessibilityDepartmentId() == null || !departmentService.exists(form.getAccessibilityDepartmentId())) {
            form.setAccessibilityDepartmentId(null);
        }

        // Remove the developing department id if it does not exist
        if (form.getDevelopingDepartmentId() == null || !departmentService.exists(form.getDevelopingDepartmentId())) {
            form.setDevelopingDepartmentId(null);
        }

        // Remove the managing department id if it does not exist
        if (form.getManagingDepartmentId() == null || !departmentService.exists(form.getManagingDepartmentId())) {
            form.setManagingDepartmentId(null);
        }

        // Remove the responsible department id if it does not exist
        if (form.getResponsibleDepartmentId() == null || !departmentService.exists(form.getResponsibleDepartmentId())) {
            form.setResponsibleDepartmentId(null);
        }

        // Remove the theme id if it does not exist
        if (form.getThemeId() == null || !themeService.exists(form.getThemeId())) {
            form.setThemeId(null);
        }

        // Remove the pdf body template key if it does not exist
        if (form.getPdfBodyTemplateKey() == null || StringUtils.isNullOrEmpty(form.getPdfBodyTemplateKey()) || !assetService.exists(form.getPdfBodyTemplateKey())) {
            form.setPdfBodyTemplateKey(null);
        }

        // Remove the payment provider if it does not exist ior is not enabled
        if (form.getPaymentProvider() == null || StringUtils.isNullOrEmpty(form.getPaymentProvider())) {
            var paymentProvider = paymentProviderService
                    .retrieve(form.getPaymentProvider());
            if (paymentProvider.isEmpty() || !paymentProvider.get().getIsEnabled()) {
                form.setPaymentProvider(null);
            }
        }

        // Remove all non-existing identity providers from the list of linked identity providers
        var cleanedIdentityProvider = new LinkedList<IdentityProviderLink>();
        for (var link : form.getIdentityProviders()) {
            if (link.getIdentityProviderKey() != null && identityProviderService.exists(link.getIdentityProviderKey())) {
                cleanedIdentityProvider.add(link);
            }
        }
        form.setIdentityProviders(cleanedIdentityProvider);

        // Check if an identity is required but the list ist empty. Reset the requirement if this is the case
        if (form.getIdentityRequired() && form.getIdentityProviders().isEmpty()) {
            form.setIdentityRequired(false);
        }

        return form;
    }

    @Override
    public void performDelete(@Nonnull Form form) throws ResponseException {
        if (form.getStatus() == FormStatus.Published) {
            throw new ResponseException(HttpStatus.CONFLICT, "Veröffentlichte Formulare können nicht gelöscht werden");
        }

        var submissionSpec = SpecificationBuilder
                .create(Submission.class)
                .withEquals("formId", form.getId())
                .withNotEquals("status", SubmissionStatus.Archived)
                .withEquals("isTestSubmission", false)
                .build();

        if (submissionService.exists(submissionSpec)) {
            throw new ResponseException(HttpStatus.CONFLICT, "Das Formular kann nicht gelöscht werden, da noch offene Anträge vorhanden sind");
        }

        var submissionsToDeleteSpec = SpecificationBuilder
                .create(Submission.class)
                .withEquals("formId", form.getId())
                .withEquals("isTestSubmission", true)
                .build();

        var submissions = submissionRepository
                .findAll(submissionsToDeleteSpec);

        for (var submission : submissions) {
            submissionService.performDelete(submission);
        }

        repository.delete(form);
    }

    @Nonnull
    public List<FormPublishChecklistItem> getFormPublishChecklist(@Nonnull Form existingForm) {
        var checklist = new LinkedList<FormPublishChecklistItem>();

        checklist.add(
                FormPublishChecklistItem
                        .create()
                        .setLabel("Öffentlicher Titel & Überschrift hinterlegt")
                        .setDone(StringUtils.isNotNullOrEmpty(existingForm.getRoot().getHeadline()))
        );

        checklist.add(
                FormPublishChecklistItem
                        .create()
                        .setLabel("Fachlicher Support eingerichtet")
                        .setDone(existingForm.getLegalSupportDepartmentId() != null)
        );

        checklist.add(
                FormPublishChecklistItem
                        .create()
                        .setLabel("Technischer Support eingerichtet")
                        .setDone(existingForm.getTechnicalSupportDepartmentId() != null)
        );

        checklist.add(
                FormPublishChecklistItem
                        .create()
                        .setLabel("Impressum eingerichtet")
                        .setDone(existingForm.getImprintDepartmentId() != null)
        );

        checklist.add(
                FormPublishChecklistItem
                        .create()
                        .setLabel("Datenschutzerklärung eingerichtet")
                        .setDone(existingForm.getPrivacyDepartmentId() != null)
        );

        checklist.add(
                FormPublishChecklistItem
                        .create()
                        .setLabel("Barrierefreiheitserklärung eingerichtet")
                        .setDone(existingForm.getAccessibilityDepartmentId() != null)
        );

        checklist.add(
                FormPublishChecklistItem
                        .create()
                        .setLabel("Zuständige und/oder bewirtschaftende Stelle eingerichtet")
                        .setDone(existingForm.getManagingDepartmentId() != null || existingForm.getResponsibleDepartmentId() != null)
        );

        checklist.add(
                FormPublishChecklistItem
                        .create()
                        .setLabel("Löschfrist für Anträge festgelegt")
                        .setDone(existingForm.getSubmissionDeletionWeeks() != null && existingForm.getSubmissionDeletionWeeks() >= 0)
        );

        checklist.add(
                FormPublishChecklistItem
                        .create()
                        .setLabel("Zugriffszeit für Bürger:innen festgelegt")
                        .setDone(existingForm.getCustomerAccessHours() != null && existingForm.getCustomerAccessHours() >= 0)
        );

        if (StringUtils.isNotNullOrEmpty(existingForm.getPaymentProvider())) {
            checklist.add(
                    FormPublishChecklistItem
                            .create()
                            .setLabel("Beinhaltet ausschließlich produktive Zahlungsdienstleister")
                            .setDone(
                                    paymentProviderService.exists(existingForm.getPaymentProvider()) &&
                                    !paymentProviderService.isTestProvider(existingForm.getPaymentProvider()) // TODO: Mit einem Filter + Exists lösen statt zwei DB calls
                            )
            );
        }

        if (existingForm.getIdentityProviders() != null && !existingForm.getIdentityProviders().isEmpty()) {
            var allLinkedIDPsProductive = existingForm
                    .getIdentityProviders()
                    .stream()
                    .allMatch(idp -> (
                            idp.getIdentityProviderKey() != null &&
                            identityProviderService.exists(
                                    new IdentityProviderFilter()
                                            .setKey(idp.getIdentityProviderKey())
                                            .setIsTestProvider(false)
                                            .setIsEnabled(true)
                            )
                    ));

            checklist.add(
                    FormPublishChecklistItem
                            .create()
                            .setLabel("Beinhaltet ausschließlich produktive Nutzerkontenanbieter")
                            .setDone(allLinkedIDPsProductive)
            );
        }

        if (existingForm.getIdentityRequired() && (existingForm.getIdentityProviders() == null || existingForm.getIdentityProviders().isEmpty())) {
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
                        .setLabel("Alle Elemente getestet")
                        .setDone(ElementApprovalService.isApproved(existingForm.getRoot()))
        );

        return checklist;
    }

    @Nonnull
    public Form publish(@Nonnull Form existingForm) throws ResponseException {
        var allChecklistItemsDone = getFormPublishChecklist(existingForm)
                .stream()
                .allMatch(FormPublishChecklistItem::getDone);

        if (!allChecklistItemsDone) {
            throw new ResponseException(HttpStatus.CONFLICT, "Das Formular kann nicht veröffentlicht werden, da nicht alle Voraussetzungen erfüllt sind");
        }

        existingForm.setStatus(FormStatus.Published);

        return repository.save(existingForm);
    }

    @Nonnull
    public Form revoke(@Nonnull Form existingForm) throws ResponseException {
        if (existingForm.getStatus() != FormStatus.Published) {
            throw new ResponseException(HttpStatus.CONFLICT, "Das Formular ist nicht veröffentlicht und kann nicht zurückgezogen werden");
        }

        existingForm.setStatus(FormStatus.Revoked);
        return repository.save(existingForm);
    }
}
