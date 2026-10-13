package de.aivot.prosuna.backend.plugins.core.v1.payment;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import de.aivot.prosuna.backend.asset.repositories.VStorageIndexItemWithAssetRepository;
import de.aivot.prosuna.backend.elements.enums.AssetVisibility;
import de.aivot.prosuna.backend.elements.models.elements.form.input.AssetSelectInputElement;
import de.aivot.prosuna.backend.secrets.services.SecretService;
import de.aivot.prosuna.backend.storage.services.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class EPayBLPaymentProviderDefinitionV1Test {
    @ParameterizedTest
    @CsvSource({
            "2026-10-08T11:42:14.181Z, 2026-10-08T09:42:14.181Z",
            "2026-01-15T12:00:00Z, 2026-01-15T11:00:00Z",
            "2026-07-15T12:00:00+00:00, 2026-07-15T10:00:00Z",
            "2026-10-08T11:42:14.181+02:00, 2026-10-08T09:42:14.181Z",
            "2026-01-15T12:00:00+01:00, 2026-01-15T11:00:00Z",
            "2026-10-08T11:42:14.123456789Z, 2026-10-08T09:42:14.123456789Z",
            "2026-10-08T00:30:00Z, 2026-10-07T22:30:00Z",
            "2026-10-25T02:30:00+02:00, 2026-10-25T00:30:00Z",
            "2026-10-25T02:30:00+01:00, 2026-10-25T01:30:00Z"
    })
    void correctsTransactionTimestamp(String timestamp, String expected) {
        assertEquals(expected, createDefinition().fixTransactionTimestamp(timestamp));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void leavesMissingTransactionTimestampUnset(String timestamp) {
        assertNull(createDefinition().fixTransactionTimestamp(timestamp));
    }

    @ParameterizedTest
    @CsvSource({
            "2026-03-29T02:30:00Z, nonexistent",
            "2026-10-25T02:30:00Z, ambiguous",
            "invalid, could not be parsed",
            "2026-10-08T11:42:14.181, could not be parsed"
    })
    void logsOriginalValueAndLeavesUnreliableTimestampUnset(String timestamp, String reason) {
        var logger = (Logger) LoggerFactory.getLogger(ePayBLPaymentProviderDefinitionV1.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);

        try {
            assertNull(createDefinition().fixTransactionTimestamp(timestamp));

            assertEquals(1, appender.list.size());
            var event = appender.list.getFirst();
            assertEquals(Level.WARN, event.getLevel());
            assertTrue(event.getFormattedMessage().contains(timestamp));
            assertTrue(event.getFormattedMessage().contains(reason));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private static ePayBLPaymentProviderDefinitionV1 createDefinition() {
        return new ePayBLPaymentProviderDefinitionV1(
                mock(SecretService.class),
                mock(StorageService.class),
                mock(VStorageIndexItemWithAssetRepository.class)
        );
    }

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
