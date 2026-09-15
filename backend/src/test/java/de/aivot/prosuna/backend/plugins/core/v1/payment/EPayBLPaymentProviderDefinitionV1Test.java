package de.aivot.prosuna.backend.plugins.core.v1.payment;

import de.aivot.prosuna.backend.asset.repositories.VStorageIndexItemWithAssetRepository;
import de.aivot.prosuna.backend.elements.enums.AssetVisibility;
import de.aivot.prosuna.backend.elements.models.elements.form.input.AssetSelectInputElement;
import de.aivot.prosuna.backend.secrets.services.SecretService;
import de.aivot.prosuna.backend.storage.services.StorageService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class EPayBLPaymentProviderDefinitionV1Test {
    @Test
    void paymentConfigLoadsCertificatesThroughTheAssetSelector() throws Exception {
        var assetRepository = mock(VStorageIndexItemWithAssetRepository.class);
        var definition = new ePayBLPaymentProviderDefinitionV1(
                mock(SecretService.class),
                mock(StorageService.class),
                assetRepository
        );

        var layout = definition.getPaymentConfigLayout();
        var certificateInput = layout
                .findChild("certificate", AssetSelectInputElement.class)
                .orElseThrow();

        assertTrue(certificateInput.getRequired());
        assertEquals("ePayBL Zertifikat", certificateInput.getPlaceholder());
        assertEquals("ePayBL Zertifikat auswählen", certificateInput.getDialogTitle());
        assertEquals(List.of("application/x-pkcs12"), certificateInput.getAllowedMimeTypes());
        assertEquals(AssetVisibility.All, certificateInput.getAssetVisibility());
        verifyNoInteractions(assetRepository);
    }
}
