package de.aivot.prosuna.backend.payment.services;

import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.enums.XBezahldienstStatus;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.payment.entities.PaymentProviderEntity;
import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.prosuna.backend.payment.exceptions.PaymentException;
import de.aivot.prosuna.backend.payment.models.PaymentPayload;
import de.aivot.prosuna.backend.payment.models.PaymentProviderDefinition;
import de.aivot.prosuna.backend.payment.models.PaymentTransactionChangeListener;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentInformation;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentRequest;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentTransaction;
import de.aivot.prosuna.backend.payment.repositories.PaymentProviderRepository;
import de.aivot.prosuna.backend.payment.repositories.PaymentTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PaymentTransactionServiceTest {
    private static final String ORIGINAL_TIMESTAMP = "2026-10-08T11:42:14.181Z";
    private static final String CORRECTED_TIMESTAMP = "2026-10-08T09:42:14.181Z";
    private static final String REQUEST_TIMESTAMP = "2026-10-08T09:40:00Z";

    private final PaymentTransactionRepository repository = mock(PaymentTransactionRepository.class);
    private final PaymentProviderDefinitionsService definitions = mock(PaymentProviderDefinitionsService.class);
    private final PaymentProviderRepository providers = mock(PaymentProviderRepository.class);
    private final PaymentProviderConfigurationService configuration = mock(PaymentProviderConfigurationService.class);
    private final PaymentProviderDefinition definition = mock(PaymentProviderDefinition.class);
    private final PaymentTransactionChangeListener listener = mock(PaymentTransactionChangeListener.class);
    private final DerivedRuntimeElementData derivedConfiguration = mock(DerivedRuntimeElementData.class);
    private final PaymentProviderEntity provider = new PaymentProviderEntity()
            .setKey(UUID.randomUUID())
            .setPaymentProviderDefinitionKey("test-provider")
            .setPaymentProviderDefinitionVersion(1);
    private final AtomicInteger completion = new AtomicInteger(-1);

    private PaymentTransactionService service;

    @BeforeEach
    void setUp() throws Exception {
        when(definitions.getProviderDefinition("test-provider", 1)).thenReturn(Optional.of(definition));
        when(providers.findById(provider.getKey())).thenReturn(Optional.of(provider));
        when(configuration.deriveConfiguration(provider, definition)).thenReturn(derivedConfiguration);
        var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        service = new PaymentTransactionService(
                List.of(listener), mock(ProsunaConfig.class), repository, mock(AuditService.class, RETURNS_DEEP_STUBS),
                definitions, providers, configuration, new DataSourceTransactionManager(source)
        );
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = CORRECTED_TIMESTAMP)
    void correctsTimestampBeforeSavingNewTransaction(String correctedTimestamp) throws Exception {
        var payload = new PaymentPayload();
        var request = paymentRequest();
        var response = providerResponse(XBezahldienstStatus.INITIAL);
        when(definition.createPaymentRequest(eq(provider), eq(derivedConfiguration), eq(payload), anyString()))
                .thenReturn(request);
        when(definition.initiatePayment(provider, derivedConfiguration, request)).thenReturn(response);
        when(definition.fixTransactionTimestamp(ORIGINAL_TIMESTAMP)).thenReturn(correctedTimestamp);
        assertTimestampWhenSaved(correctedTimestamp, XBezahldienstStatus.INITIAL);

        var transaction = service.create(provider, payload, "https://example.com/return/");

        assertEquals(correctedTimestamp, transaction.getPaymentInformation().getTransactionTimestamp());
        assertSame(request, transaction.getPaymentRequest());
        assertEquals(REQUEST_TIMESTAMP, request.getRequestTimestamp());
        verify(definition).fixTransactionTimestamp(ORIGINAL_TIMESTAMP);
        verify(repository).save(transaction);
        verifyNoInteractions(listener);
    }

    @ParameterizedTest
    @CsvSource({
            "false, 2026-10-08T09:42:14.181Z",
            "true, 2026-10-08T09:42:14.181Z",
            "false,",
            "true,"
    })
    void correctsTimestampBeforeSavingAndNotifyingOnStatusChange(boolean push, String correctedTimestamp) throws Exception {
        var transaction = storedTransaction(XBezahldienstStatus.INITIAL);
        var previousInformation = transaction.getPaymentInformation();
        var callbackData = mockCallback(push, XBezahldienstStatus.PAYED);
        when(definition.fixTransactionTimestamp(ORIGINAL_TIMESTAMP)).thenReturn(correctedTimestamp);
        assertTimestampWhenSaved(correctedTimestamp, XBezahldienstStatus.PAYED);
        doAnswer(invocation -> {
            var notified = invocation.getArgument(0, PaymentTransactionEntity.class);
            assertSame(transaction, notified);
            assertEquals(correctedTimestamp, notified.getPaymentInformation().getTransactionTimestamp());
            assertEquals(XBezahldienstStatus.PAYED, notified.getStatus());
            verify(repository).save(notified);
            return null;
        }).when(listener).onChange(any());

        service.processCallback(transaction, callbackData);

        assertEquals(CORRECTED_TIMESTAMP, previousInformation.getTransactionTimestamp());
        assertEquals(correctedTimestamp, transaction.getPaymentInformation().getTransactionTimestamp());
        assertEquals(REQUEST_TIMESTAMP, transaction.getPaymentRequest().getRequestTimestamp());
        assertNull(transaction.getPaymentError());
        verify(definition).fixTransactionTimestamp(ORIGINAL_TIMESTAMP);
        verify(listener).onChange(transaction);

        service.processCallback(transaction, callbackData);

        assertEquals(correctedTimestamp, transaction.getPaymentInformation().getTransactionTimestamp());
        verify(definition).fixTransactionTimestamp(nullable(String.class));
        verify(repository).save(transaction);
        verify(listener).onChange(transaction);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void doesNotCorrectOrSaveWhenStatusIsUnchanged(boolean push) throws Exception {
        var transaction = storedTransaction(XBezahldienstStatus.PAYED);
        var previousInformation = transaction.getPaymentInformation();
        var callbackData = mockCallback(push, XBezahldienstStatus.PAYED);

        service.processCallback(transaction, callbackData);

        assertSame(previousInformation, transaction.getPaymentInformation());
        assertEquals(CORRECTED_TIMESTAMP, previousInformation.getTransactionTimestamp());
        verify(definition, org.mockito.Mockito.never()).fixTransactionTimestamp(nullable(String.class));
        verifyNoInteractions(repository, listener);
    }

    // Listeners request follow-up work, which must commit or roll back together with the payment change.
    @Test
    void savesTheNewStatusAndNotifiesListenersInOneTransaction() throws Exception {
        var transaction = storedTransaction(XBezahldienstStatus.INITIAL);
        mockCallback(false, XBezahldienstStatus.PAYED);
        recordCompletionWhenSaved();
        doAnswer(call -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            return null;
        }).when(listener).onChange(transaction);

        service.processCallback(transaction, null);

        var order = inOrder(repository, listener);
        order.verify(repository).save(transaction);
        order.verify(listener).onChange(transaction);
        assertEquals(TransactionSynchronization.STATUS_COMMITTED, completion.get());
    }

    @Test
    void failingListenerRollsBackTheStatusChange() throws Exception {
        var transaction = storedTransaction(XBezahldienstStatus.INITIAL);
        mockCallback(false, XBezahldienstStatus.PAYED);
        recordCompletionWhenSaved();
        doThrow(ResponseException.internalServerError("Fehler")).when(listener).onChange(transaction);

        assertThrows(PaymentException.class, () -> service.processCallback(transaction, null));

        assertEquals(TransactionSynchronization.STATUS_ROLLED_BACK, completion.get());
    }

    @Test
    void deletesTheTransactionAndNotifiesListenersInOneTransaction() throws Exception {
        var transaction = storedTransaction(XBezahldienstStatus.INITIAL);
        doAnswer(call -> {
            recordCompletion();
            return null;
        }).when(listener).onDelete(transaction);
        doAnswer(call -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            return null;
        }).when(repository).delete(transaction);

        service.performDelete(transaction);

        var order = inOrder(listener, repository);
        order.verify(listener).onDelete(transaction);
        order.verify(repository).delete(transaction);
        assertEquals(TransactionSynchronization.STATUS_COMMITTED, completion.get());
    }

    @Test
    void failingDeleteListenerKeepsTheTransaction() throws Exception {
        var transaction = storedTransaction(XBezahldienstStatus.INITIAL);
        doAnswer(call -> {
            recordCompletion();
            throw ResponseException.internalServerError("Fehler");
        }).when(listener).onDelete(transaction);

        var exception = assertThrows(ResponseException.class, () -> service.performDelete(transaction));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        verify(repository, never()).delete(any(PaymentTransactionEntity.class));
        assertEquals(TransactionSynchronization.STATUS_ROLLED_BACK, completion.get());
    }

    private void recordCompletionWhenSaved() {
        when(repository.save(any())).thenAnswer(call -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            recordCompletion();
            return call.getArgument(0);
        });
    }

    private void recordCompletion() {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                completion.set(status);
            }
        });
    }

    private Map<String, Object> mockCallback(boolean push, XBezahldienstStatus status) throws Exception {
        if (push) {
            Map<String, Object> callbackData = Map.of("transactionId", "provider-transaction");
            when(definition.onPaymentResultPush(eq(provider), eq(derivedConfiguration), any(), eq(callbackData)))
                    .thenAnswer(ignored -> providerResponse(status));
            return callbackData;
        }
        when(definition.onPaymentResultPull(eq(provider), eq(derivedConfiguration), any()))
                .thenAnswer(ignored -> providerResponse(status));
        return null;
    }

    private void assertTimestampWhenSaved(String expectedTimestamp, XBezahldienstStatus expectedStatus) {
        when(repository.save(any())).thenAnswer(invocation -> {
            var transaction = invocation.getArgument(0, PaymentTransactionEntity.class);
            assertEquals(expectedTimestamp, transaction.getPaymentInformation().getTransactionTimestamp());
            assertEquals(expectedStatus, transaction.getStatus());
            assertEquals("provider-transaction", transaction.getPaymentInformation().getTransactionId());
            return transaction;
        });
    }

    private PaymentTransactionEntity storedTransaction(XBezahldienstStatus status) {
        var information = new XBezahldienstePaymentInformation();
        information.setStatus(status);
        information.setTransactionTimestamp(CORRECTED_TIMESTAMP);
        return new PaymentTransactionEntity()
                .setKey("transaction")
                .setPaymentProviderKey(provider.getKey())
                .setPaymentRequest(paymentRequest())
                .setPaymentInformation(information);
    }

    private static XBezahldienstePaymentRequest paymentRequest() {
        var request = new XBezahldienstePaymentRequest();
        request.setRequestTimestamp(REQUEST_TIMESTAMP);
        return request;
    }

    private static XBezahldienstePaymentTransaction providerResponse(XBezahldienstStatus status) {
        var information = new XBezahldienstePaymentInformation();
        information.setStatus(status);
        information.setTransactionId("provider-transaction");
        information.setTransactionTimestamp(ORIGINAL_TIMESTAMP);
        var response = new XBezahldienstePaymentTransaction();
        response.setPaymentInformation(information);
        return response;
    }
}
