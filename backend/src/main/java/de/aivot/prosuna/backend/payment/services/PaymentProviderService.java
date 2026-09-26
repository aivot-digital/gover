package de.aivot.prosuna.backend.payment.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.lib.services.EntityService;
import de.aivot.prosuna.backend.payment.entities.PaymentProviderEntity;
import de.aivot.prosuna.backend.payment.filters.PaymentTransactionFilter;
import de.aivot.prosuna.backend.payment.models.PaymentProviderDefinition;
import de.aivot.prosuna.backend.payment.repositories.PaymentProviderRepository;
import de.aivot.prosuna.backend.payment.repositories.PaymentTransactionRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentProviderService implements EntityService<PaymentProviderEntity, UUID> {
    private final PaymentProviderRepository paymentProviderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentTransactionService paymentTransactionService;
    private final PaymentProviderDefinitionsService paymentProviderDefinitionsService;

    @Autowired
    public PaymentProviderService(PaymentProviderRepository paymentProviderRepository,
                                  PaymentTransactionRepository paymentTransactionRepository,
                                  PaymentTransactionService paymentTransactionService,
                                  PaymentProviderDefinitionsService paymentProviderDefinitionsService) {
        this.paymentProviderRepository = paymentProviderRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.paymentTransactionService = paymentTransactionService;
        this.paymentProviderDefinitionsService = paymentProviderDefinitionsService;
    }

    @Nonnull
    public Optional<PaymentProviderDefinition> getProviderDefinition(@Nonnull String providerKey, @Nonnull Integer providerVersion) {
        return paymentProviderDefinitionsService.getProviderDefinition(providerKey, providerVersion);
    }

    @Nonnull
    @Override
    public PaymentProviderEntity create(
            @Nonnull PaymentProviderEntity paymentProviderEntity
    ) throws ResponseException {
        // Retrieve the payment provider definition
        getProviderDefinition(
                paymentProviderEntity.getPaymentProviderDefinitionKey(),
                paymentProviderEntity.getPaymentProviderDefinitionVersion()
        ).orElseThrow(() -> new ResponseException(
                HttpStatus.BAD_REQUEST,
                "Der ausgewählte Zahlungsanbieter in Version %d ist nicht vorhanden"
                        .formatted(paymentProviderEntity.getPaymentProviderDefinitionVersion())
        ));

        // Create new key for the payment provider entity
        paymentProviderEntity.setKey(UUID.randomUUID());

        // Save and return the payment provider entity
        return paymentProviderRepository.save(paymentProviderEntity);
    }

    @Nonnull
    @Override
    public Page<PaymentProviderEntity> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<PaymentProviderEntity> specification,
            Filter<PaymentProviderEntity> filter) {
        return paymentProviderRepository
                .findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<PaymentProviderEntity> retrieve(
            @Nonnull UUID key
    ) {
        return paymentProviderRepository
                .findById(key);
    }

    @Nonnull
    @Override
    public Optional<PaymentProviderEntity> retrieve(
            @Nonnull Specification<PaymentProviderEntity> specification
    ) {
        return paymentProviderRepository
                .findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull UUID id) {
        return paymentProviderRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<PaymentProviderEntity> specification) {
        return paymentProviderRepository.exists(specification);
    }

    @Nonnull
    @Override
    public PaymentProviderEntity performUpdate(
            @Nonnull UUID id,
            @Nonnull PaymentProviderEntity entity,
            @Nonnull PaymentProviderEntity existingEntity
    ) throws ResponseException {
        // Retrieve the payment provider definition
        if (!Objects.equals(existingEntity.getPaymentProviderDefinitionKey(), entity.getPaymentProviderDefinitionKey()) ||
                !Objects.equals(existingEntity.getPaymentProviderDefinitionVersion(), entity.getPaymentProviderDefinitionVersion())) {
            throw new ResponseException(
                    HttpStatus.BAD_REQUEST,
                    "Der ausgewählte Zahlungsanbieter und seine Version können nach der Erstellung nicht mehr geändert werden"
            );
        }

        getProviderDefinition(
                existingEntity.getPaymentProviderDefinitionKey(),
                existingEntity.getPaymentProviderDefinitionVersion()
        ).orElseThrow(() -> new ResponseException(
                HttpStatus.BAD_REQUEST,
                "Der ausgewählte Zahlungsanbieter in Version %d ist nicht vorhanden"
                        .formatted(existingEntity.getPaymentProviderDefinitionVersion())
        ));

        // Update the existing payment provider entity
        existingEntity.setName(entity.getName());
        existingEntity.setDescription(entity.getDescription());
        // Do not update the provider key or version because changing them can break existing transactions.
        existingEntity.setConfig(entity.getConfig());
        existingEntity.setIsEnabled(entity.getIsEnabled());
        existingEntity.setTestProvider(entity.getTestProvider());

        return paymentProviderRepository
                .save(existingEntity);
    }

    @Override
    public void performDelete(
            @Nonnull PaymentProviderEntity entity
    ) throws ResponseException {
        // TODO: Check if this payment provider is still referenced in a process node config and prevent deletion if so.

        if (entity.getIsEnabled()) {
            throw ResponseException.conflict(
                    "Der Zahlungsanbieter %s (%s) ist noch aktiviert. Bitte deaktivieren Sie den Anbieter, bevor Sie ihn löschen.",
                    entity.getName(),
                    entity.getKey()
            );
        }

        var transactionFilter = PaymentTransactionFilter
                .create()
                .setPaymentProviderKey(entity.getKey())
                .build();

        var transactions = paymentTransactionRepository
                .findAll(transactionFilter);

        for (var transaction : transactions) {
            paymentTransactionService
                    .performDelete(transaction);
        }

        paymentProviderRepository.delete(entity);
    }

    public boolean isTestProvider(UUID providerKey) {
        return paymentProviderRepository.findById(providerKey)
                .map(PaymentProviderEntity::getTestProvider)
                .orElse(false);
    }
}
