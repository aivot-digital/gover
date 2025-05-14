package de.aivot.GoverBackend.payment.services;

import de.aivot.GoverBackend.form.filters.FormFilter;
import de.aivot.GoverBackend.form.repositories.FormRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.payment.entities.PaymentProviderEntity;
import de.aivot.GoverBackend.payment.filters.PaymentTransactionFilter;
import de.aivot.GoverBackend.payment.models.PaymentProviderDefinition;
import de.aivot.GoverBackend.payment.repositories.PaymentProviderRepository;
import de.aivot.GoverBackend.payment.repositories.PaymentTransactionRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

@Service
public class PaymentProviderService implements EntityService<PaymentProviderEntity, String> {
    private final PaymentProviderRepository paymentProviderRepository;
    private final FormRepository formRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentTransactionService paymentTransactionService;
    private final PaymentProviderDefinitionsService paymentProviderDefinitionsService;

    @Autowired
    public PaymentProviderService(
            PaymentProviderRepository paymentProviderRepository,
            FormRepository formRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            PaymentTransactionService paymentTransactionService,
            PaymentProviderDefinitionsService paymentProviderDefinitionsService) {
        this.formRepository = formRepository;
        this.paymentProviderRepository = paymentProviderRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.paymentTransactionService = paymentTransactionService;
        this.paymentProviderDefinitionsService = paymentProviderDefinitionsService;
    }

    @Nonnull
    public Optional<PaymentProviderDefinition> getProviderDefinition(@Nonnull String providerKey) {
        return paymentProviderDefinitionsService.getProviderDefinition(providerKey);
    }

    @Nonnull
    @Override
    public PaymentProviderEntity create(
            @Nonnull PaymentProviderEntity paymentProviderEntity
    ) throws ResponseException {
        // Retrieve the payment provider definition
        getProviderDefinition(paymentProviderEntity.getProviderKey())
                .orElseThrow(() -> new ResponseException(HttpStatus.BAD_REQUEST, "Der ausgewählte Zahlungsanbieter ist nicht vorhanden"));

        // Create new key for the payment provider entity
        paymentProviderEntity.setKey(UUID.randomUUID().toString());

        // Save and return the payment provider entity
        return paymentProviderRepository.save(paymentProviderEntity);
    }

    @NotNull
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
            @Nonnull String key
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
    public boolean exists(@Nonnull String id) {
        return paymentProviderRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<PaymentProviderEntity> specification) {
        return paymentProviderRepository.exists(specification);
    }

    @Nonnull
    @Override
    public PaymentProviderEntity performUpdate(
            @Nonnull String id,
            @Nonnull PaymentProviderEntity entity,
            @Nonnull PaymentProviderEntity existingEntity
    ) throws ResponseException {
        // Retrieve the payment provider definition
        var providerDefinition = getProviderDefinition(entity.getProviderKey())
                .orElseThrow(() -> new ResponseException(HttpStatus.BAD_REQUEST, "Der ausgewählte Zahlungsanbieter ist nicht vorhanden"));

        // Test if the provider key is valid
        if (providerDefinition == null) {
            throw new ResponseException(HttpStatus.BAD_REQUEST, "Der ausgewählte Zahlungsanbieter ist nicht vorhanden");
        }

        // Update the existing payment provider entity
        existingEntity.setName(entity.getName());
        existingEntity.setDescription(entity.getDescription());
        // Do not update the provider key because changing the provider key can break existing transactions
        // existingEntity.setProviderKey(entity.getProviderKey());
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
        var formSpec = new FormFilter()
                .setPaymentProvider(entity.getKey())
                .build();

        if (formRepository.exists(formSpec)) {
            throw ResponseException.conflict(
                    "Der Zahlungsanbieter %s (%s) wird noch in Formularen verwendet",
                    entity.getName(),
                    entity.getKey()
            );
        }

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

    public boolean isTestProvider(String providerKey) {
        return paymentProviderRepository.findById(providerKey)
                .map(PaymentProviderEntity::getTestProvider)
                .orElse(false);
    }
}
