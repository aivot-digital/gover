package de.aivot.GoverBackend.form.services;

import de.aivot.GoverBackend.department.services.DepartmentService;
import de.aivot.GoverBackend.enums.SubmissionStatus;
import de.aivot.GoverBackend.form.entities.FormEntity;
import de.aivot.GoverBackend.form.entities.FormSlugHistoryEntity;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.repositories.FormRepository;
import de.aivot.GoverBackend.form.repositories.FormSlugHistoryRepository;
import de.aivot.GoverBackend.form.repositories.FormVersionRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.submission.entities.Submission;
import de.aivot.GoverBackend.submission.repositories.SubmissionRepository;
import de.aivot.GoverBackend.submission.services.SubmissionService;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.function.ThrowingBiFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

@Service
public class FormService implements EntityService<FormEntity, Integer> {
    private final FormRepository repository;

    private final DepartmentService departmentService;
    private final SubmissionService submissionService;
    private final SubmissionRepository submissionRepository;
    private final FormVersionRepository formVersionRepository;
    private final FormSlugHistoryRepository formSlugHistoryRepository;

    @Autowired
    public FormService(
            FormRepository repository,
            DepartmentService departmentService,
            SubmissionService submissionService,
            SubmissionRepository submissionRepository,
            FormVersionRepository formVersionRepository,
            FormSlugHistoryRepository formSlugHistoryRepository) {
        this.repository = repository;
        this.departmentService = departmentService;
        this.submissionService = submissionService;
        this.submissionRepository = submissionRepository;
        this.formVersionRepository = formVersionRepository;
        this.formSlugHistoryRepository = formSlugHistoryRepository;
    }

    @Nonnull
    @Override
    public Page<FormEntity> performList(@Nonnull Pageable pageable, @Nullable Specification<FormEntity> specification, Filter<FormEntity> filter) {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public FormEntity create(@Nonnull FormEntity entity) throws ResponseException {
        var cleanedEntity = cleanForm(null, entity);

        cleanedEntity.setId(null);
        cleanedEntity.setVersionCount(0);
        cleanedEntity.setDraftedVersion(null);
        cleanedEntity.setPublishedVersion(null);
        cleanedEntity.setCreated(null);
        cleanedEntity.setUpdated(null);
        cleanedEntity.setInternalTitle(cleanedEntity.getInternalTitle().strip());

        return repository.save(cleanedEntity);
    }

    @Nonnull
    @Override
    public FormEntity performUpdate(@Nonnull Integer id, @Nonnull FormEntity updatedForm, @Nonnull FormEntity existingForm) throws ResponseException {
        var cleanedForm = cleanForm(existingForm, updatedForm);

        existingForm.setSlug(updatedForm.getSlug());
        existingForm.setInternalTitle(cleanedForm.getInternalTitle().strip());

        existingForm.setDevelopingDepartmentId(cleanedForm.getDevelopingDepartmentId());

        if (formSlugHistoryRepository.existsById(existingForm.getSlug())) {
            formSlugHistoryRepository.deleteById(existingForm.getSlug());
        } else {
            var historyEntry = new FormSlugHistoryEntity();
            historyEntry.setSlug(existingForm.getSlug());
            historyEntry.setFormId(existingForm.getId());
            formSlugHistoryRepository.save(historyEntry);
        }

        return repository.save(existingForm);
    }

    @Nonnull
    @Override
    public Optional<FormEntity> retrieve(@Nonnull Integer id) {
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<FormEntity> retrieve(@Nonnull Specification<FormEntity> specification) {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<FormEntity> specification) {
        return repository.exists(specification);
    }

    private FormEntity cleanForm(@Nullable FormEntity prev, @Nonnull FormEntity updated) throws ResponseException {
        ThrowingBiFunction<Function<FormEntity, Integer>, Consumer<Integer>, Boolean> checkDepartment = (getter, setter) -> {
            var updatedDepartmentId = getter.apply(updated);
            if (updatedDepartmentId == null) {
                setter.accept(null);
                return false;
            } else {
                var previousDepartmentId = prev != null ? getter.apply(prev) : null;
                var previousIsSameAsUpdated = Objects.equals(previousDepartmentId, updatedDepartmentId);

                if (previousIsSameAsUpdated) {
                    setter.accept(updatedDepartmentId);
                    return true;
                } else {
                    if (departmentService.exists(updatedDepartmentId)) {
                        setter.accept(updatedDepartmentId);
                        return true;
                    } else {
                        throw ResponseException.badRequest("Der Fachbereich mit der ID " + updatedDepartmentId + " existiert nicht");
                    }
                }
            }
        };

        var notNull = checkDepartment.apply(FormEntity::getDevelopingDepartmentId, updated::setDevelopingDepartmentId);
        if (!notNull) {
            throw ResponseException.badRequest("Der entwickelnde Fachbereich ist erforderlich");
        }

        if (prev == null) {
            if (repository.existsBySlug(updated.getSlug())) {
                throw new ResponseException(HttpStatus.CONFLICT, "Es existiert bereits ein Formular mit dieser URL");
            }
            if (formSlugHistoryRepository.existsById(updated.getSlug())) {
                throw new ResponseException(HttpStatus.CONFLICT, "Es existiert bereits ein Formular, dass diese URL zuvor verwendet hat");
            }
        } else {
            if (repository.existsBySlugAndIdIsNot(updated.getSlug(), prev.getId())) {
                throw new ResponseException(HttpStatus.CONFLICT, "Es existiert bereits ein Formular mit dieser URL");
            }
            if (formSlugHistoryRepository.existsBySlugAndFormIdIsNot(updated.getSlug(), prev.getId())) {
                throw new ResponseException(HttpStatus.CONFLICT, "Es existiert bereits ein Formular, dass diese URL zuvor verwendet hat");
            }
        }

        return updated;
    }

    @Override
    public void performDelete(@Nonnull FormEntity form) throws ResponseException {
        var publishedFormVersionsExist = formVersionRepository
                .existsByFormIdAndStatus(form.getId(), FormStatus.Published);

        if (publishedFormVersionsExist) {
            throw new ResponseException(HttpStatus.CONFLICT, "Formulare mit einer veröffentlichten Version können nicht gelöscht werden");
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
}
