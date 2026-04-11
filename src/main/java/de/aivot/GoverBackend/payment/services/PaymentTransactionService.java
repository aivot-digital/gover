package de.aivot.GoverBackend.payment.services;

import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.enums.XBezahldienstStatus;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.DeleteEntityService;
import de.aivot.GoverBackend.lib.services.ReadEntityService;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.payment.entities.PaymentProviderEntity;
import de.aivot.GoverBackend.payment.entities.PaymentTransactionEntity;
import de.aivot.GoverBackend.payment.exceptions.PaymentException;
import de.aivot.GoverBackend.payment.filters.PaymentTransactionFilter;
import de.aivot.GoverBackend.payment.models.PaymentItem;
import de.aivot.GoverBackend.payment.models.PaymentProviderDefinition;
import de.aivot.GoverBackend.payment.models.PaymentTransactionChangeListener;
import de.aivot.GoverBackend.payment.models.XBezahldienstePaymentRequest;
import de.aivot.GoverBackend.payment.models.XBezahldienstePaymentTransaction;
import de.aivot.GoverBackend.payment.repositories.PaymentProviderRepository;
import de.aivot.GoverBackend.payment.repositories.PaymentTransactionRepository;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nonnull;
import java.time.LocalDateTime;
import java.util.*;

@Service
@EnableScheduling
public class PaymentTransactionService implements
        ReadEntityService<PaymentTransactionEntity, String>,
        DeleteEntityService<PaymentTransactionEntity, String> {
    private final ScopedAuditService auditService;

    private final List<PaymentTransactionChangeListener> paymentTransactionChangeListeners;

    private final GoverConfig config;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentProviderDefinitionsService paymentProviderDefinitionsService;
    private final PaymentProviderRepository paymentProviderRepository;
    private final PaymentProviderConfigurationService paymentProviderConfigurationService;

    @Autowired
    public PaymentTransactionService(
            List<PaymentTransactionChangeListener> paymentTransactionChangeListeners,
            GoverConfig config,
            PaymentTransactionRepository paymentTransactionRepository,
            AuditService auditService,
            PaymentProviderDefinitionsService paymentProviderDefinitionsService,
            PaymentProviderRepository paymentProviderRepository,
            PaymentProviderConfigurationService paymentProviderConfigurationService) {
        this.auditService = auditService.createScopedAuditService(PaymentTransactionService.class, "Zahlungen");
        this.paymentTransactionChangeListeners = paymentTransactionChangeListeners;
        this.config = config;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.paymentProviderDefinitionsService = paymentProviderDefinitionsService;
        this.paymentProviderRepository = paymentProviderRepository;
        this.paymentProviderConfigurationService = paymentProviderConfigurationService;
    }

    /**
     * Create a new payment transaction.
     * A payment transaction is only saved, when the payment request was successfully created and the payment process was successfully initiated.
     * Otherwise, a PaymentException is logged and thrown.
     *
     * @param paymentProviderEntity The payment provider the transaction should be created for
     * @param purpose               The purpose of the payment
     * @param description           The description of the payment
     * @param finalRedirectUrl      The URL to redirect to after the payment has been processed
     * @param paymentItems          The items to be paid
     * @return The created payment transaction
     * @throws PaymentException If an error occurs during the payment process
     */
    @Nonnull
    public PaymentTransactionEntity create(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull String purpose,
            @Nonnull String description,
            @Nonnull String finalRedirectUrl,
            @Nonnull List<PaymentItem> paymentItems
    ) throws PaymentException {
        // Fetch corresponding payment provider definition
        var paymentProviderDefinition = paymentProviderDefinitionsService
                .getProviderDefinition(
                        paymentProviderEntity.getPaymentProviderDefinitionKey(),
                        paymentProviderEntity.getPaymentProviderDefinitionVersion()
                )
                .orElseThrow(() -> new PaymentException(
                        "Für den Zahlungsdienstleister %s in Version %d wurde keine Definition gefunden.",
                        paymentProviderEntity.getPaymentProviderDefinitionKey(),
                        paymentProviderEntity.getPaymentProviderDefinitionVersion()
                ));

        // Prepare transaction entity
        var transactionEntity = new PaymentTransactionEntity();
        transactionEntity.setKey(UUID.randomUUID().toString());
        transactionEntity.setPaymentProviderKey(paymentProviderEntity.getKey());
        transactionEntity.setRedirectUrl(finalRedirectUrl);
        transactionEntity.setCreated(LocalDateTime.now());
        transactionEntity.setUpdated(LocalDateTime.now());

        // Create initial redirect URL
        var initialRedirectUrl = config.createUrl("/api/public/payment-transaction-callback/", transactionEntity.getKey()) + "/redirect/";

        var derivedConfiguration = derivePaymentProviderConfiguration(paymentProviderEntity, paymentProviderDefinition);

        // Create and set payment request
        XBezahldienstePaymentRequest paymentRequest;
        try {
            paymentRequest = paymentProviderDefinition.createPaymentRequest(
                    paymentProviderEntity,
                    derivedConfiguration,
                    purpose,
                    description,
                    paymentItems,
                    initialRedirectUrl
            );
        } catch (PaymentException e) {
            // Log exception and rethrow
            var metadata = new HashMap<String, Object>(Map.of(
                    "paymentProviderKey", paymentProviderEntity.getKey(),
                    "purpose", purpose,
                    "description", description,
                    "paymentItems", paymentItems
            ));
            metadata.put("exceptionType", e.getClass().getName());
            auditService.create()
                    .setTriggerType("Exception")
                    .setMessage("Die Zahlungsanfrage konnte für den Zahlungsdienstleister nicht erstellt werden.")
                    .setMetadata(metadata).log();
            throw e;
        }
        transactionEntity.setPaymentRequest(paymentRequest);

        // Initiate and set payment
        XBezahldienstePaymentTransaction xBezahldienstePaymentTransaction;
        try {
            xBezahldienstePaymentTransaction = paymentProviderDefinition.initiatePayment(
                    paymentProviderEntity,
                    derivedConfiguration,
                    transactionEntity.getPaymentRequest()
            );
        } catch (PaymentException e) {
            // Log exception and rethrow
            var metadata = new HashMap<String, Object>(Map.of(
                    "paymentProviderKey", paymentProviderEntity.getKey(),
                    "purpose", purpose,
                    "description", description,
                    "paymentItems", paymentItems
            ));
            metadata.put("exceptionType", e.getClass().getName());
            auditService.create()
                    .setTriggerType("Exception")
                    .setMessage("Die Zahlung konnte beim Zahlungsdienstleister nicht initialisiert werden.")
                    .setMetadata(metadata).log();
            throw e;
        }
        transactionEntity.setPaymentInformation(xBezahldienstePaymentTransaction.getPaymentInformation());

        return paymentTransactionRepository.save(transactionEntity);
    }

    @Nonnull
    @Override
    public Page<PaymentTransactionEntity> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<PaymentTransactionEntity> specification,
            Filter<PaymentTransactionEntity> filter) {
        return paymentTransactionRepository
                .findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<PaymentTransactionEntity> retrieve(
            @Nonnull String transactionKey
    ) {
        return paymentTransactionRepository
                .findById(transactionKey);
    }

    @Nonnull
    @Override
    public Optional<PaymentTransactionEntity> retrieve(@Nonnull Specification<PaymentTransactionEntity> specification) {
        return paymentTransactionRepository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull String id) {
        return paymentTransactionRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<PaymentTransactionEntity> specification) {
        return paymentTransactionRepository.exists(specification);
    }

    @Override
    public void performDelete(@Nonnull PaymentTransactionEntity entity) throws ResponseException {
        for (var listener : paymentTransactionChangeListeners) {
            try {
                listener.onDelete(entity);
            } catch (ResponseException e) {
                throw ResponseException.internalServerError(e, "Error notifying change listener for transaction %s", entity.getKey());
            }
        }

        paymentTransactionRepository.delete(entity);
    }

    public void processCallback(
            @Nonnull PaymentTransactionEntity transaction,
            @Nullable Map<String, Object> callbackData
    ) throws PaymentException {
        // Fetch corresponding payment provider entity. If not found, set error and throw exception
        var provider = paymentProviderRepository
                .findById(transaction.getPaymentProviderKey())
                .orElse(null);
        if (provider == null) {
            var error = new PaymentException("Der referenzierte Zahlungsdienstleister \"%s\" konnte nicht gefunden werden.", transaction.getPaymentProviderKey());
            transaction.setPaymentError(error.getMessage());
            paymentTransactionRepository.save(transaction);
            throw error;
        }

        // Fetch corresponding payment provider definition. If not found, set error and throw exception
        var providerDefinition = paymentProviderDefinitionsService
                .getProviderDefinition(
                        provider.getPaymentProviderDefinitionKey(),
                        provider.getPaymentProviderDefinitionVersion()
                )
                .orElse(null);
        if (providerDefinition == null) {
            var error = new PaymentException(
                    "Die Definition \"%s\" in Version %d des referenzierten Zahlungsdienstleisters \"%s\" konnte nicht gefunden werden.",
                    provider.getPaymentProviderDefinitionKey(),
                    provider.getPaymentProviderDefinitionVersion(),
                    provider.getKey()
            );
            transaction.setPaymentError(error.getMessage());
            paymentTransactionRepository.save(transaction);
            throw error;
        }

        // Create a new transaction object with the data from the transaction entity
        var xBezahldiensteTransaction = new XBezahldienstePaymentTransaction();
        xBezahldiensteTransaction.setPaymentRequest(transaction.getPaymentRequest());
        xBezahldiensteTransaction.setPaymentInformation(transaction.getPaymentInformation());

        var derivedConfiguration = derivePaymentProviderConfiguration(provider, providerDefinition);

        // Try to check the payment status. If an error occurs, set the error message on the transaction and rethrow the exception
        XBezahldienstePaymentTransaction xBezahldiensteTransactionUpdated;
        try {
            xBezahldiensteTransactionUpdated = callbackData != null ?
                    providerDefinition
                            .onPaymentResultPush(
                                    provider,
                                    derivedConfiguration,
                                    xBezahldiensteTransaction,
                                    callbackData
                            ) :
                    providerDefinition
                            .onPaymentResultPull(
                                    provider,
                                    derivedConfiguration,
                                    xBezahldiensteTransaction
                            );
        } catch (PaymentException e) {
            transaction.setPaymentError(e.getMessage());
            paymentTransactionRepository.save(transaction);
            throw e;
        }

        // Check if the payment status has changed
        var originalPaymentStatus = transaction.getPaymentInformation().getStatus();
        var updatedPaymentStatus = xBezahldiensteTransactionUpdated.getPaymentInformation().getStatus();

        var paymentStatusChanged = !Objects.equals(originalPaymentStatus, updatedPaymentStatus);

        // If the payment status has changed, update the transaction and notify all listeners
        if (paymentStatusChanged) {
            // Update and save the transaction
            transaction.setPaymentInformation(xBezahldiensteTransactionUpdated.getPaymentInformation());
            var updatedTransaction = paymentTransactionRepository.save(transaction);

            // Iterate over all listeners and notify them about the change
            for (var listener : paymentTransactionChangeListeners) {
                try {
                    listener.onChange(updatedTransaction);
                } catch (ResponseException e) {
                    throw new PaymentException(e, "Error notifying change listener for transaction %s", updatedTransaction.getKey());
                }
            }
        }
    }

    @Scheduled(cron = "* 0/15 * * * *", zone = "Europe/Paris")
    public void poll() {
        var spec = PaymentTransactionFilter
                .create()
                .setStatus(XBezahldienstStatus.INITIAL)
                .setHasError(false)
                .build();

        var pendingTransactions = paymentTransactionRepository
                .findAll(spec);

        for (var transactionEntity : pendingTransactions) {
            auditService.create()
                    .setTriggerType("Debug")
                    .setMessage("Der Status der Zahlungstransaktion mit dem Schlüssel " + transactionEntity.getKey() + " wird abgefragt.")
                    .setMetadata(Map.of("transactionKey", transactionEntity.getKey())).log();

            try {
                processCallback(transactionEntity, null);
            } catch (PaymentException e) {
                auditService.create()
                        .setTriggerType("Exception")
                        .setMessage("Beim Abfragen der Zahlungstransaktion mit dem Schlüssel " + transactionEntity.getKey() + " ist ein Fehler aufgetreten.")
                        .setMetadata(Map.of(
                                "transactionKey", transactionEntity.getKey(),
                                "exceptionType", e.getClass().getName()
                        )).log();
                // TODO: Set error flag on transaction
            }
        }
    }

    @Nonnull
    private DerivedRuntimeElementData derivePaymentProviderConfiguration(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull PaymentProviderDefinition paymentProviderDefinition
    ) throws PaymentException {
        try {
            return paymentProviderConfigurationService
                    .deriveConfiguration(paymentProviderEntity, paymentProviderDefinition);
        } catch (ResponseException e) {
            throw new PaymentException(
                    e,
                    "Die Konfiguration des Zahlungsanbieters %s (%s) konnte nicht abgeleitet werden.",
                    paymentProviderEntity.getName(),
                    paymentProviderEntity.getKey()
            );
        }
    }
}
