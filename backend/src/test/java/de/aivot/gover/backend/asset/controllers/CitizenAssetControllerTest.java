package de.aivot.gover.backend.asset.controllers;

import de.aivot.gover.backend.asset.entities.VStorageIndexItemWithAssetEntityId;
import de.aivot.gover.backend.asset.repositories.VStorageIndexItemWithAssetRepository;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.storage.services.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CitizenAssetControllerTest {
    @Mock
    private StorageService storageService;

    @Mock
    private VStorageIndexItemWithAssetRepository vStorageIndexItemWithAssetRepository;

    @Mock
    private HttpServletRequest request;

    private CitizenAssetController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        controller = new CitizenAssetController(
                storageService,
                vStorageIndexItemWithAssetRepository
        );
    }

    @Test
    void retrievePublicByPathPreservesPlusInFilePath() {
        when(request.getRequestURL())
                .thenReturn(new StringBuffer("http://localhost/api/public/assets/42/files/folder/a+b.txt"));
        when(vStorageIndexItemWithAssetRepository.findById(VStorageIndexItemWithAssetEntityId.of(42, "/folder/a+b.txt")))
                .thenReturn(Optional.empty());

        assertThrows(
                ResponseException.class,
                () -> controller.retrievePublicByPath(42, false, request)
        );

        verify(vStorageIndexItemWithAssetRepository)
                .findById(VStorageIndexItemWithAssetEntityId.of(42, "/folder/a+b.txt"));
    }
}
