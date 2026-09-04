package de.aivot.prosuna.backend.payment.services;

import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.audit.services.ScopedAuditService;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.lib.services.DeleteEntityService;
import de.aivot.prosuna.backend.lib.services.ReadEntityService;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.payment.entities.PaymentProviderEntity;
import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.prosuna.backend.payment.exceptions.PaymentException;
import de.aivot.prosuna.backend.payment.filters.PaymentTransactionFilter;
import de.aivot.prosuna.backend.payment.models.*;
import de.aivot.prosuna.backend.payment.repositories.PaymentProviderRepository;
import de.aivot.prosuna.backend.payment.repositories.PaymentTransactionRepository;
import de.aivot.prosuna.backend.utils.RandomUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@EnableScheduling
public class PaymentTransactionService implements
        ReadEntityService<PaymentTransactionEntity, String>,
        DeleteEntityService<PaymentTransactionEntity, String> {
    private final ScopedAuditService auditService;

    private final List<PaymentTransactionChangeListener> paymentTransactionChangeListeners;

    private final ProsunaConfig config;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentProviderDefinitionsService paymentProviderDefinitionsService;
    private final PaymentProviderRepository paymentProviderRepository;
    private final PaymentProviderConfigurationService paymentProviderConfigurationService;

    @Autowired
    public PaymentTransactionService(
            List<PaymentTransactionChangeListener> paymentTransactionChangeListeners,
            ProsunaConfig config,
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
     * @param payload               The payment data to submit
     * @param finalRedirectUrl      The URL to redirect to after the payment has been processed
     * @return The created payment transaction
     * @throws PaymentException If an error occurs during the payment process
     */
    @Nonnull
    public PaymentTransactionEntity create(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull PaymentPayload payload,
            @Nonnull String finalRedirectUrl
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
        transactionEntity.setKey(RandomUtils.generateRandomString(PaymentTransactionEntity.KEY_LENGTH));
        transactionEntity.setPaymentProviderKey(paymentProviderEntity.getKey());
        transactionEntity.setRedirectUrl(finalRedirectUrl);
        transactionEntity.setCreated(Instant.now());
        transactionEntity.setUpdated(Instant.now());

        // Create initial redirect URL
        var initialRedirectUrl = config.createUrl("/api/public/payment-transaction-callback/", transactionEntity.getKey()) + "/redirect/";

        var derivedConfiguration = derivePaymentProviderConfiguration(paymentProviderEntity, paymentProviderDefinition);

        // Create and set payment request
        PaymentRequest paymentRequest;
        try {
            paymentRequest = paymentProviderDefinition.createPaymentRequest(
                    paymentProviderEntity,
                    derivedConfiguration,
                    payload,
                    initialRedirectUrl
            );
        } catch (PaymentException e) {
            // Log exception and rethrow
            var metadata = new HashMap<String, Object>(Map.of(
                    "paymentProviderKey", paymentProviderEntity.getKey(),
                    "purpose", payload.getPurpose(),
                    "description", payload.getDescription(),
                    "paymentItems", payload.getPaymentItems()
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
        PaymentInformation paymentInformation;
        try {
            paymentInformation = paymentProviderDefinition.initiatePayment(
                    paymentProviderEntity,
                    derivedConfiguration,
                    transactionEntity.getPaymentRequest()
            );
        } catch (PaymentException e) {
            // Log exception and rethrow
            var metadata = new HashMap<String, Object>(Map.of(
                    "paymentProviderKey", paymentProviderEntity.getKey(),
                    "purpose", payload.getPurpose(),
                    "description", payload.getDescription(),
                    "paymentItems", payload.getPaymentItems()
            ));
            metadata.put("exceptionType", e.getClass().getName());
            auditService.create()
                    .setTriggerType("Exception")
                    .setMessage("Die Zahlung konnte beim Zahlungsdienstleister nicht initialisiert werden.")
                    .setMetadata(metadata).log();
            throw e;
        }
        transactionEntity.setPaymentInformation(paymentInformation);

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
    public Optional<PaymentTransactionEntity> retrieveByRedirectUrl(@Nonnull String redirectUrl) {
        return paymentTransactionRepository
                .findFirstByRedirectUrlOrderByCreatedDesc(redirectUrl);
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

        var derivedConfiguration = derivePaymentProviderConfiguration(provider, providerDefinition);

        // Try to check the payment status. If an error occurs, set the error message on the transaction and rethrow the exception
        PaymentInformation updatedPaymentInformation;
        try {
            updatedPaymentInformation = callbackData != null ?
                    providerDefinition
                            .onPaymentResultPush(
                                    provider,
                                    derivedConfiguration,
                                    transaction.getPaymentInformation(),
                                    callbackData
                            ) :
                    providerDefinition
                            .onPaymentResultPull(
                                    provider,
                                    derivedConfiguration,
                                    transaction.getPaymentInformation()
                            );
        } catch (PaymentException e) {
            transaction.setPaymentError(e.getMessage());
            paymentTransactionRepository.save(transaction);
            throw e;
        }

        // Check if the payment status has changed
        var originalPaymentStatus = transaction.getPaymentInformation().status();
        var updatedPaymentStatus = updatedPaymentInformation.status();

        var paymentStatusChanged = !Objects.equals(originalPaymentStatus, updatedPaymentStatus);
        var paymentInformationChanged = !Objects.equals(transaction.getPaymentInformation(), updatedPaymentInformation);

        // Persist all information changes, but notify listeners only about status transitions.
        if (paymentInformationChanged) {
            // Update and save the transaction
            transaction.setPaymentInformation(updatedPaymentInformation);
            var updatedTransaction = paymentTransactionRepository.save(transaction);

            if (paymentStatusChanged) {
                for (var listener : paymentTransactionChangeListeners) {
                    try {
                        listener.onChange(updatedTransaction);
                    } catch (ResponseException e) {
                        throw new PaymentException(e, "Error notifying change listener for transaction %s", updatedTransaction.getKey());
                    }
                }
            }
        }
    }

    @Scheduled(fixedRate = 15, timeUnit = TimeUnit.MINUTES)
    public void poll() {
        var spec = PaymentTransactionFilter
                .create()
                .setStatus(PaymentStatus.PENDING)
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
