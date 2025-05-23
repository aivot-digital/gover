package de.aivot.GoverBackend.services;

import de.aivot.GoverBackend.data.SpecialCustomerInputKeys;
import de.aivot.GoverBackend.elements.models.BaseElement;
import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.elements.models.form.BaseFormElement;
import de.aivot.GoverBackend.elements.models.form.BaseInputElement;
import de.aivot.GoverBackend.elements.models.form.input.FileUploadField;
import de.aivot.GoverBackend.elements.models.form.layout.GroupLayout;
import de.aivot.GoverBackend.elements.models.form.layout.ReplicatingContainerLayout;
import de.aivot.GoverBackend.elements.models.steps.StepElement;
import de.aivot.GoverBackend.form.entities.Form;
import de.aivot.GoverBackend.identity.constants.IdentityValueKey;
import de.aivot.GoverBackend.identity.models.IdentityValue;
import de.aivot.GoverBackend.payment.entities.PaymentProviderEntity;
import de.aivot.GoverBackend.payment.entities.PaymentTransactionEntity;
import de.aivot.GoverBackend.submission.entities.Submission;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Consumer;


public class DestinationDataFormatter {
    private final Map<String, Object> data;
    private static final String destinationSkipKey = "#";

    private final Form form;
    private final Submission submission;
    private final PaymentTransactionEntity paymentTransaction;
    private final PaymentProviderEntity paymentProvider;
    private final byte[] pdfBytes;
    private final Map<String, byte[]> attachmentBytes;

    private DestinationDataFormatter(
            @Nonnull
            Form form,
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
            Form form,
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
            Form form,
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
        insertValue("form.name", form.getTitle());
        insertValue("form.slug", form.getSlug());
        insertValue("form.version", form.getVersion());
        insertValue("form.headline", form.getFormTitle());
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
        var rawIdpData = submission
                .getCustomerInput()
                .get(IdentityValueKey.IdCustomerInputKey);

        if (rawIdpData instanceof Map<?,?> mRawIdpData) {
            IdentityValue identityValue;
            try {
                identityValue = IdentityValue
                        .fromMap(mRawIdpData);
            } catch (IllegalArgumentException e) {
                insertValue("authentication.is_authenticated", false);
                return;
            }

            insertValue("authentication.is_authenticated", true);
            insertValue("authentication.identity_provider", identityValue.identityProviderKey());
            insertValue("authentication.data", identityValue.userInfo());
        }
    }

    private void createCustomerData() {
        Map<String, Object> customerData = new HashMap<>();
        extractDataFromElement(customerData, form.getRoot(), null);
        data.put("data", customerData);
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
                insertValue("payment.provider.provider_identifier", paymentProvider.getProviderKey());
                insertValue("payment.provider.is_test_provider", paymentProvider.getTestProvider());
            }
        }
    }

    private void extractDataFromElement(Map<String, Object> resultContainer, BaseElement element, String idPrefix) {
        Consumer<BaseElement> extractChildData = (e) -> extractDataFromElement(resultContainer, e, idPrefix);

        switch (element) {
            case BaseFormElement formElement -> {
                switch (formElement) {
                    case GroupLayout groupLayout -> {
                        groupLayout.getChildren().forEach(extractChildData);
                    }
                    case ReplicatingContainerLayout replicatingContainerLayout -> extractReplicatingContainer(resultContainer, replicatingContainerLayout, idPrefix);
                    case FileUploadField fileUploadField -> extractFileUploadField(resultContainer, fileUploadField, idPrefix);
                    case BaseInputElement<?> baseInputElement -> {
                        extractBaseInput(resultContainer, baseInputElement, idPrefix);
                    }
                    default -> {
                        // Do nothing
                    }
                }
            }
            case RootElement rootElement -> rootElement.getChildren().forEach(extractChildData);
            case StepElement stepElement -> stepElement.getChildren().forEach(extractChildData);
            case null, default -> {
                // Do nothing
            }
        }
    }

    private void extractReplicatingContainer(Map<String, Object> resultContainer, ReplicatingContainerLayout element, String idPrefix) {
        var resolvedElementId = getResolvedElementId(idPrefix, element.getId());
        var rawChildIds = submission.getCustomerInput().get(resolvedElementId);

        // Check if children exist
        if (!(rawChildIds instanceof Collection<?>)) {
            return;
        }

        // Extract destination key and determine if this container should be skipped
        var elementDestinationKey = element.getDestinationKey() != null ? element.getDestinationKey() : resolvedElementId;
        if (destinationSkipKey.equals(elementDestinationKey)) {
            return;
        }

        // Create extracted child data list to store extracted data into
        var extractedChildDataList = new LinkedList<>();
        for (var childId : (Collection<?>) rawChildIds) {
            var childData = new HashMap<String, Object>();
            for (var child : element.getChildren()) {
                extractDataFromElement(childData, child, resolvedElementId + "_" + childId + "_");
            }
            extractedChildDataList.add(childData);
        }

        // Insert extracted data into result container
        insertValue(resultContainer, elementDestinationKey, extractedChildDataList);
    }

    private void extractFileUploadField(Map<String, Object> resultContainer, FileUploadField element, String idPrefix) {
        if (!includeAttachments()) {
            return;
        }

        var resolvedElementId = getResolvedElementId(idPrefix, element.getId());
        var values = submission.getCustomerInput().get(resolvedElementId);

        // Check if values is a collection and not null
        if (!(values instanceof Collection<?>)) {
            return;
        }

        // Extract destination key and determine if this element should be skipped
        var elementDestinationKey = element.getDestinationKey() != null ? element.getDestinationKey() : resolvedElementId;
        if (destinationSkipKey.equals(elementDestinationKey)) {
            return;
        }

        // Annotate file upload values with base64 encoded file content
        for (var fileUploadValueItem : (Collection<?>) values) {
            if (fileUploadValueItem instanceof Map<?, ?> fileUploadValueItemMap) {
                var fileName = fileUploadValueItemMap.get("name");

                if (fileName != null && attachmentBytes.containsKey(fileName)) {
                    var bytes = attachmentBytes.get(fileName);
                    var attachmentBase64 = loadBytesAsBase64(bytes);
                    ((Map<String, Object>) fileUploadValueItemMap).put("base64", attachmentBase64);
                }
            }
        }

        // Insert annotated values into result container
        insertValue(resultContainer, elementDestinationKey, values);
    }

    private void extractBaseInput(Map<String, Object> resultContainer, BaseInputElement<?> element, String idPrefix) {
        var resolvedElementId = getResolvedElementId(idPrefix, element.getId());
        var value = submission.getCustomerInput().get(resolvedElementId);

        // Check if value is a collection and not null
        if (value == null) {
            return;
        }

        // Extract destination key and determine if this element should be skipped
        var elementDestinationKey = element.getDestinationKey() != null ? element.getDestinationKey() : resolvedElementId;
        if (destinationSkipKey.equals(elementDestinationKey)) {
            return;
        }

        // Check if metadata user info is set and fill user data in data
        if (element.getMetadata() != null && element.getMetadata().getUserInfoIdentifier() != null) {
            var userInfoIdentifier = element.getMetadata().getUserInfoIdentifier();
            insertValue("user." + userInfoIdentifier, value);
        }

        insertValue(resultContainer, elementDestinationKey, value);
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

    private static String getResolvedElementId(String idPrefix, String element) {
        return (idPrefix != null ? idPrefix : "") + element;
    }
}
