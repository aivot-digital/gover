package de.aivot.GoverBackend.form.services;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.exceptions.BadRequestException;
import de.aivot.GoverBackend.form.entities.*;
import de.aivot.GoverBackend.form.repositories.FormRevisionRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.lib.DiffItem;
import de.aivot.GoverBackend.services.DiffService;
import de.aivot.GoverBackend.user.entities.UserEntity;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FormRevisionService {
    private final FormService formService;
    private final FormRevisionRepository formRevisionRepository;
    private final FormVersionService formVersionService;

    private static final String[] IGNORED_FIELDS = new String[] {
            "created",
            "updated",
            "internalTitle"
    };

    @Autowired
    public FormRevisionService(FormService formService,
                               FormRevisionRepository formRevisionRepository,
                               FormVersionService formVersionService) {
        this.formRevisionRepository = formRevisionRepository;
        this.formService = formService;
        this.formVersionService = formVersionService;
    }

    @Nonnull
    public Page<FormRevisionEntity> list(@Nonnull Integer formId,
                                         @Nonnull Integer formVersion,
                                         @Nonnull Pageable pageable) {
        return formRevisionRepository
                .getAllByFormIdAndFormVersionOrderByTimestampDesc(formId, formVersion, pageable);
    }

    public void create(
            @Nonnull UserEntity user,
            @Nonnull VFormVersionWithDetailsEntity updatedFormVersion
    ) {
        create(user, updatedFormVersion, null);
    }

    public void create(
            @Nonnull UserEntity user,
            @Nonnull VFormVersionWithDetailsEntity updatedFormVersion,
            @Nullable VFormVersionWithDetailsEntity existingFormVersion
    ) {
        if (existingFormVersion == null) {
            createForNewForm(user, updatedFormVersion);
        } else {
            createForExistingForm(user, updatedFormVersion, existingFormVersion);
        }
    }

    private void createForNewForm(
            @Nonnull UserEntity user,
            @Nonnull VFormVersionWithDetailsEntity createdForm
    ) {
        var formJson = new JSONObject(createdForm);
        for (String field : IGNORED_FIELDS) {
            formJson.remove(field);
            formJson.remove(field);
        }

        var formMap = formJson.toMap();

        var diff = new DiffItem("/", null, formMap);

        var formRevision = new FormRevisionEntity();
        formRevision.setFormId(createdForm.getId());
        formRevision.setFormVersion(createdForm.getVersion());
        formRevision.setUserId(user.getId());
        formRevision.setTimestamp(LocalDateTime.now());
        formRevision.setDiff(List.of(diff));

        formRevisionRepository.save(formRevision);
    }

    private void createForExistingForm(
            @Nonnull UserEntity user,
            @Nonnull VFormVersionWithDetailsEntity updatedForm,
            @Nonnull VFormVersionWithDetailsEntity existingForm
    ) {
        var updatedFormJson = new JSONObject(updatedForm);
        var existingFormJson = new JSONObject(existingForm);

        // Ignore the updated field
        for (String field : IGNORED_FIELDS) {
            updatedFormJson.remove(field);
            existingFormJson.remove(field);
        }

        var changes = DiffService.createDiff(existingFormJson, updatedFormJson);

        if (changes.isEmpty()) {
            return;
        }

        var formRevision = new FormRevisionEntity();
        formRevision.setFormId(existingForm.getId());
        formRevision.setFormVersion(existingForm.getVersion());
        formRevision.setUserId(user.getId());
        formRevision.setTimestamp(LocalDateTime.now());
        formRevision.setDiff(changes);

        formRevisionRepository.save(formRevision);
    }

    public VFormVersionWithDetailsEntity rollback(VFormVersionWithDetailsEntity form, BigInteger revisionId) throws ResponseException {
        var firstRevision = formRevisionRepository
                .getFirstByFormIdAndFormVersionOrderByTimestampAsc(form.getId(), form.getVersion())
                .orElseThrow(ResponseException::notFound);

        var targetRevisionToRollBackTo = formRevisionRepository
                .findById(revisionId)
                .orElseThrow(ResponseException::notFound);

        if (firstRevision.getId().equals(targetRevisionToRollBackTo.getId())) {
            throw ResponseException.badRequest();
        }

        var succeedingRevisionsToRollBack = formRevisionRepository
                .getAllByFormIdAndFormVersionAndTimestampIsAfterOrderByTimestampDesc(form.getId(), form.getVersion(), targetRevisionToRollBackTo.getTimestamp());

        succeedingRevisionsToRollBack.add(targetRevisionToRollBackTo);

        var formObj = new JSONObject(form);
        for (var revision : succeedingRevisionsToRollBack) {
            for (var diff : revision.getDiff()) {
                formObj = DiffService.rollBackDiff(formObj, diff);
            }
        }

        // Remove these fields to prevent jackson from crashing because of LocalDateTime
        formObj.remove("updated");
        formObj.remove("created");

        var objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        var rolledBackFormVersionWithDetails = objectMapper.convertValue(formObj.toMap(), VFormVersionWithDetailsEntity.class);

        FormEntity rolledBackForm = rolledBackFormVersionWithDetails.toFormEntity();
        FormVersionEntity rolledBackVersion = rolledBackFormVersionWithDetails.toFormVersionEntity();

        formService
                .update(form.getId(), rolledBackForm);
        var formVersionId = FormVersionEntityId
                .of(form.getId(), form.getVersion());
        formVersionService
                .update(formVersionId, rolledBackVersion);

        return rolledBackFormVersionWithDetails;
    }
}
