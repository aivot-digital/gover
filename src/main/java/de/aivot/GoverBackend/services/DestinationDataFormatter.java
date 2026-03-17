package de.aivot.GoverBackend.services;

import de.aivot.GoverBackend.form.entities.VFormVersionWithDetailsEntity;
import de.aivot.GoverBackend.payment.entities.PaymentProviderEntity;
import de.aivot.GoverBackend.payment.entities.PaymentTransactionEntity;
import de.aivot.GoverBackend.submission.entities.Submission;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;


public class DestinationDataFormatter {
    private final Map<String, Object> data;

    private final VFormVersionWithDetailsEntity form;
    private final Submission submission;
    private final PaymentTransactionEntity paymentTransaction;
    private final PaymentProviderEntity paymentProvider;
    private final byte[] pdfBytes;
    private final Map<String, byte[]> attachmentBytes;

    private DestinationDataFormatter(
            @Nonnull
            VFormVersionWithDetailsEntity form,
            @Nonnull
            Submission submission,
            @Nullable
            PaymentTransactionEntity paymentTransaction,
            @Nullable
            PaymentProviderEntity paymentProvider,
            @Nullable
            byte[] pdfBytes,
            @Nullable
            Map<String, byte[]> attachmentBytes
    ) {
        this.form = form;
        this.submission = submission;
        this.paymentTransaction = paymentTransaction;
        this.paymentProvider = paymentProvider;
        this.pdfBytes = pdfBytes;
        this.attachmentBytes = attachmentBytes;
        this.data = new HashMap<>();
    }

    public static DestinationDataFormatter createDataWithoutFiles(
            @Nonnull
            VFormVersionWithDetailsEntity form,
            @Nonnull
            Submission submission,
            @Nullable
            PaymentTransactionEntity paymentTransaction,
            @Nullable
            PaymentProviderEntity paymentProvider
    ) {
        return new DestinationDataFormatter(
                form,
                submission,
                paymentTransaction,
                paymentProvider,
                null, null
        );
    }

    public static DestinationDataFormatter create(
            @Nonnull
            VFormVersionWithDetailsEntity form,
            @Nonnull
            Submission submission,
            @Nullable
            PaymentTransactionEntity paymentTransaction,
            @Nullable
            PaymentProviderEntity paymentProvider,
            @Nonnull
            byte[] pdfBytes,
            @Nonnull
            Map<String, byte[]> attachmentBytes
    ) {
        return new DestinationDataFormatter(
                form,
                submission,
                paymentTransaction,
                paymentProvider,
                pdfBytes,
                attachmentBytes
        );
    }

    private boolean includePdf() {
        return pdfBytes != null;
    }

    private boolean includeAttachments() {
        return attachmentBytes != null;
    }

    public Map<String, Object> format() {
        // TODO: Derive Data

        createFormData();
        createMetadata();
        createAuthenticationData();
        createCustomerData();
        createPaymentData();

        if (includePdf()) {
            data.put("_pdf", loadBytesAsBase64(pdfBytes));
        }

        return data;
    }

    private void createFormData() {
        insertValue("form.id", form.getId());
        insertValue("form.name", form.getInternalTitle());
        insertValue("form.slug", form.getSlug());
        insertValue("form.version", form.getVersion());
        insertValue("form.headline", form.getPublicTitle());
        insertValue("form.managing_department_id", form.getManagingDepartmentId());
        insertValue("form.responsible_department_id", form.getResponsibleDepartmentId());
        insertValue("form.developing_department_id", form.getDevelopingDepartmentId());
    }

    private void createMetadata() {
        insertValue("metadata.submission_id", submission.getId());
        insertValue("metadata.is_test_submission", submission.getIsTestSubmission());
        insertValue("metadata.submitted", submission.getCreated().format(DateTimeFormatter.ISO_DATE_TIME));
        insertValue("metadata.user_rating", submission.getReviewScore());
    }

    private void createAuthenticationData() {
        // Authentication export is currently not part of the formatted destination payload.
    }

    private void createCustomerData() {
        // Customer field export is currently not part of the formatted destination payload.
    }

    private void createPaymentData() {
        insertValue("payment.active", paymentTransaction != null);

        if (paymentTransaction != null) {
            insertValue("payment.key", paymentTransaction.getKey());
            insertValue("payment.request_id", paymentTransaction.getPaymentRequest().getRequestId());
            insertValue("payment.purpose", paymentTransaction.getPaymentRequest().getPurpose());
            insertValue("payment.redirect_url", paymentTransaction.getPaymentInformation().getTransactionRedirectUrl());
            insertValue("payment.status", paymentTransaction.getPaymentInformation().getStatus().getKey());
            insertValue("payment.amount", paymentTransaction.getPaymentRequest().getGrosAmount());
            insertValue("payment.currency", paymentTransaction.getPaymentRequest().getCurrency());
            insertValue("payment.method", paymentTransaction.getPaymentInformation().getPaymentMethod());
            insertValue("payment.items", paymentTransaction.getPaymentRequest().getItems());

            if (paymentProvider != null) {
                insertValue("payment.provider.key", paymentProvider.getKey());
                insertValue("payment.provider.name", paymentProvider.getName());
                insertValue("payment.provider.provider_identifier", paymentProvider.getPaymentProviderDefinitionKey());
                insertValue("payment.provider.is_test_provider", paymentProvider.getTestProvider());
            }
        }
    }

    private String loadBytesAsBase64(byte[] bytes) {
        return org.apache.commons.codec.binary.Base64.encodeBase64String(bytes);
    }

    private void insertValue(String path, Object value) {
        insertValue(this.data, path, value);
    }

    private void insertValue(Map<String, Object> map, String path, Object value) {
        var currentMap = map;
        var pathLayers = path.split("\\.");
        for (int i = 0; i < pathLayers.length - 1; i++) {
            var layer = pathLayers[i];
            if (!currentMap.containsKey(layer)) {
                currentMap.put(layer, new HashMap<String, Object>());
            }
            currentMap = (Map<String, Object>) currentMap.get(layer);
        }
        currentMap.put(pathLayers[pathLayers.length - 1], value);
    }

}
