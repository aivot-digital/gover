package de.aivot.GoverBackend.form.services;

import de.aivot.GoverBackend.department.services.DepartmentService;
import de.aivot.GoverBackend.form.entities.FormEntity;
import de.aivot.GoverBackend.form.entities.FormSlugHistoryEntity;
import de.aivot.GoverBackend.form.repositories.FormRepository;
import de.aivot.GoverBackend.form.repositories.FormSlugHistoryRepository;
import de.aivot.GoverBackend.form.repositories.FormVersionRepository;
import de.aivot.GoverBackend.submission.repositories.SubmissionRepository;
import de.aivot.GoverBackend.submission.services.SubmissionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FormServiceTest {
    @Test
    void performUpdateShouldStorePreviousSlugInHistory() throws Exception {
        var repository = mock(FormRepository.class);
        var departmentService = mock(DepartmentService.class);
        var submissionService = mock(SubmissionService.class);
        var submissionRepository = mock(SubmissionRepository.class);
        var formVersionRepository = mock(FormVersionRepository.class);
        var formSlugHistoryRepository = mock(FormSlugHistoryRepository.class);

        var service = new FormService(
                repository,
                departmentService,
                submissionService,
                submissionRepository,
                formVersionRepository,
                formSlugHistoryRepository
        );

        var existingForm = new FormEntity()
                .setId(12)
                .setSlug("old-slug")
                .setInternalTitle("Altes Formular")
                .setDevelopingDepartmentId(3)
                .setVersionCount(1)
                .setCreated(LocalDateTime.now().minusDays(1))
                .setUpdated(LocalDateTime.now().minusHours(1));

        var updatedForm = new FormEntity()
                .setSlug("new-slug")
                .setInternalTitle("Neues Formular")
                .setDevelopingDepartmentId(3);

        when(repository.existsBySlugAndIdIsNot("new-slug", 12)).thenReturn(false);
        when(formSlugHistoryRepository.existsBySlugAndFormIdIsNot("new-slug", 12)).thenReturn(false);
        when(formSlugHistoryRepository.existsById("new-slug")).thenReturn(false);
        when(repository.save(any(FormEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var savedForm = service.performUpdate(12, updatedForm, existingForm);

        assertEquals("new-slug", savedForm.getSlug());
        var historyCaptor = ArgumentCaptor.forClass(FormSlugHistoryEntity.class);
        verify(formSlugHistoryRepository, times(1)).save(historyCaptor.capture());
        assertNotNull(historyCaptor.getValue());
        assertEquals("old-slug", historyCaptor.getValue().getSlug());
        assertEquals(12, historyCaptor.getValue().getFormId());
        verify(formSlugHistoryRepository, never()).deleteById("new-slug");
        verify(formSlugHistoryRepository, never()).deleteById("old-slug");
    }
}
