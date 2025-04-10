package de.aivot.GoverBackend.form.services;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.exceptions.BadRequestException;
import de.aivot.GoverBackend.form.entities.Form;
import de.aivot.GoverBackend.form.entities.FormRevision;
import de.aivot.GoverBackend.form.repositories.FormRevisionRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.lib.DiffItem;
import de.aivot.GoverBackend.services.DiffService;
import de.aivot.GoverBackend.user.entities.UserEntity;
import org.json.JSONObject;
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
    private final FormRevisionRepository formRevisionRepository;
    private final FormService formService;

    public FormRevisionService(FormRevisionRepository formRevisionRepository, FormService formService) {
        this.formRevisionRepository = formRevisionRepository;
        this.formService = formService;
    }

    @Nonnull
    public Page<FormRevision> list(@Nonnull Integer formId, @Nonnull Pageable pageable) {
        return formRevisionRepository
                .getAllByFormIdOrderByTimestampDesc(formId, pageable);
    }

    public void create(
            @Nonnull UserEntity user,
            @Nonnull Form updatedForm,
            @Nullable Form existingForm
    ) {
        if (existingForm == null) {
            createForNewForm(user, updatedForm);
        } else {
            createForExistingForm(user, updatedForm, existingForm);
        }
    }

    private void createForNewForm(
            @Nonnull UserEntity user,
            @Nonnull Form createdForm
    ) {
        var formJson = new JSONObject(createdForm);
        var formMap = formJson.toMap();

        var diff = new DiffItem("/", null, formMap);

        var formRevision = new FormRevision();
        formRevision.setFormId(createdForm.getId());
        formRevision.setUserId(user.getId());
        formRevision.setTimestamp(LocalDateTime.now());
        formRevision.setDiff(List.of(diff));

        formRevisionRepository.save(formRevision);
    }

    private void createForExistingForm(
            @Nonnull UserEntity user,
            @Nonnull Form updatedForm,
            @Nonnull Form existingForm
    ) {
        var updatedFormJson = new JSONObject(updatedForm);
        var existingFormJson = new JSONObject(existingForm);

        // Ignore the updated field
        updatedFormJson.remove("updated");
        existingFormJson.remove("updated");

        var changes = DiffService.createDiff(existingFormJson, updatedFormJson);

        if (changes.isEmpty()) {
            return;
        }

        var formRevision = new FormRevision();
        formRevision.setFormId(existingForm.getId());
        formRevision.setUserId(user.getId());
        formRevision.setTimestamp(LocalDateTime.now());
        formRevision.setDiff(changes);

        formRevisionRepository.save(formRevision);
    }

    public Form rollback(Form form, BigInteger revisionId) throws ResponseException {
        var firstRevision = formRevisionRepository
                .getFirstByFormIdOrderByTimestampAsc(form.getId())
                .orElseThrow(ResponseException::notFound);

        var targetRevisionToRollBackTo = formRevisionRepository
                .findById(revisionId)
                .orElseThrow(ResponseException::notFound);

        if (firstRevision.getId().equals(targetRevisionToRollBackTo.getId())) {
            throw new BadRequestException();
        }

        var succeedingRevisionsToRollBack = formRevisionRepository
                .getAllByFormIdAndTimestampIsAfterOrderByTimestampDesc(form.getId(), targetRevisionToRollBackTo.getTimestamp());

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
        var rolledBackForm = objectMapper.convertValue(formObj.toMap(), Form.class);

        return formService
                .update(form.getId(), rolledBackForm);
    }
}
