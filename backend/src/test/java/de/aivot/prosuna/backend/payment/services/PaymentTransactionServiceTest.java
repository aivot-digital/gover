package de.aivot.prosuna.backend.payment.services;

import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.enums.XBezahldienstStatus;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.payment.entities.PaymentProviderEntity;
import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.prosuna.backend.payment.exceptions.PaymentException;
import de.aivot.prosuna.backend.payment.models.PaymentProviderDefinition;
import de.aivot.prosuna.backend.payment.models.PaymentTransactionChangeListener;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentInformation;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentTransaction;
import de.aivot.prosuna.backend.payment.repositories.PaymentProviderRepository;
import de.aivot.prosuna.backend.payment.repositories.PaymentTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Listeners request follow-up work, which must commit or roll back together with the payment change. */
class PaymentTransactionServiceTest {
    private final UUID providerKey = UUID.randomUUID();
    private final PaymentTransactionRepository transactions = mock(PaymentTransactionRepository.class);
    private final PaymentProviderRepository providers = mock(PaymentProviderRepository.class);
    private final PaymentProviderDefinitionsService definitions = mock(PaymentProviderDefinitionsService.class);
    private final PaymentProviderConfigurationService configurations = mock(PaymentProviderConfigurationService.class);
    private final PaymentProviderDefinition definition = mock(PaymentProviderDefinition.class);
    private final PaymentTransactionChangeListener listener = mock(PaymentTransactionChangeListener.class);
    private final AtomicInteger completion = new AtomicInteger(-1);
    private PaymentTransactionService service;
    private PaymentTransactionEntity transaction;

    @BeforeEach
    void setup() throws Exception {
        var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        service = new PaymentTransactionService(List.of(listener), mock(ProsunaConfig.class), transactions,
                mock(AuditService.class, RETURNS_DEEP_STUBS), definitions, providers, configurations,
                new DataSourceTransactionManager(source));
        transaction = new PaymentTransactionEntity().setKey("tx-1").setPaymentProviderKey(providerKey)
                .setPaymentInformation(information(XBezahldienstStatus.INITIAL));
        var provider = new PaymentProviderEntity().setKey(providerKey).setPaymentProviderDefinitionKey("provider")
                .setPaymentProviderDefinitionVersion(1);
        when(providers.findById(providerKey)).thenReturn(Optional.of(provider));
        when(definitions.getProviderDefinition("provider", 1)).thenReturn(Optional.of(definition));
        when(configurations.deriveConfiguration(provider, definition)).thenReturn(new DerivedRuntimeElementData());
        var paid = new XBezahldienstePaymentTransaction();
        paid.setPaymentInformation(information(XBezahldienstStatus.PAYED));
        when(definition.onPaymentResultPull(eq(provider), any(), any())).thenReturn(paid);
        when(transactions.save(transaction)).thenAnswer(call -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            recordCompletion();
            return transaction;
        });
    }

    @Test
    void savesTheNewStatusAndNotifiesListenersInOneTransaction() throws Exception {
        doAnswer(call -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            return null;
        }).when(listener).onChange(transaction);

        service.processCallback(transaction, null);

        var order = inOrder(transactions, listener);
        order.verify(transactions).save(transaction);
        order.verify(listener).onChange(transaction);
        assertEquals(TransactionSynchronization.STATUS_COMMITTED, completion.get());
    }

    @Test
    void failingListenerRollsBackTheStatusChange() throws Exception {
        doThrow(ResponseException.internalServerError("Fehler")).when(listener).onChange(transaction);

        assertThrows(PaymentException.class, () -> service.processCallback(transaction, null));

        assertEquals(TransactionSynchronization.STATUS_ROLLED_BACK, completion.get());
    }

    @Test
    void deletesTheTransactionAndNotifiesListenersInOneTransaction() throws Exception {
        doAnswer(call -> {
            recordCompletion();
            return null;
        }).when(listener).onDelete(transaction);
        doAnswer(call -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            return null;
        }).when(transactions).delete(transaction);

        service.performDelete(transaction);

        var order = inOrder(listener, transactions);
        order.verify(listener).onDelete(transaction);
        order.verify(transactions).delete(transaction);
        assertEquals(TransactionSynchronization.STATUS_COMMITTED, completion.get());
    }

    @Test
    void failingDeleteListenerKeepsTheTransaction() throws Exception {
        doAnswer(call -> {
            recordCompletion();
            throw ResponseException.internalServerError("Fehler");
        }).when(listener).onDelete(transaction);

        var exception = assertThrows(ResponseException.class, () -> service.performDelete(transaction));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        verify(transactions, never()).delete(any(PaymentTransactionEntity.class));
        assertEquals(TransactionSynchronization.STATUS_ROLLED_BACK, completion.get());
    }

    private void recordCompletion() {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                completion.set(status);
            }
        });
    }

    private static XBezahldienstePaymentInformation information(XBezahldienstStatus status) {
        var information = new XBezahldienstePaymentInformation();
        information.setStatus(status);
        return information;
    }
}
