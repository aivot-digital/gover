package de.aivot.GoverBackend.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.core.converters.ElementDataConverter;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.ElementDataObject;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.BaseFormElement;
import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.FileUploadInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.steps.GenericStepElement;
import de.aivot.GoverBackend.form.entities.VFormVersionWithDetailsEntity;
import de.aivot.GoverBackend.identity.constants.IdentityValueKey;
import de.aivot.GoverBackend.identity.models.IdentityData;
import de.aivot.GoverBackend.payment.entities.PaymentProviderEntity;
import de.aivot.GoverBackend.payment.entities.PaymentTransactionEntity;
import de.aivot.GoverBackend.submission.entities.Submission;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Consumer;


public class DestinationDataFormatter {
    private static final Logger logger = LoggerFactory.getLogger(DestinationDataFormatter.class);

    private final Map<String, Object> data;
    private static final String destinationSkipKey = "#";

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
        ElementDataObject rawIdpData = submission
                .getCustomerInput()
                .get(IdentityValueKey.IdCustomerInputKey);

        if (rawIdpData == null || rawIdpData.getValue() == null) {
            insertValue("authentication.is_authenticated", false);
            return;
        }

        IdentityData identityValue = null;
        try {
            identityValue = new ObjectMapper()
                    .convertValue(rawIdpData.getValue(), IdentityData.class);
        } catch (IllegalArgumentException e) {
            logger.error("Could not convert IdentityData to IdentityData", e);
        }

        if (identityValue == null) {
            insertValue("authentication.is_authenticated", false);
            return;
        }

        insertValue("authentication.is_authenticated", true);
        insertValue("authentication.identity_provider", identityValue.providerKey());
        insertValue("authentication.data", identityValue.attributes());
    }

    private void createCustomerData() {
        Map<String, Object> customerData = new HashMap<>();
        extractDataFromElement(customerData, form.getRootElement(), submission.getCustomerInput());
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
                insertValue("payment.provider.provider_identifier", paymentProvider.getPaymentProviderDefinitionKey());
                insertValue("payment.provider.is_test_provider", paymentProvider.getTestProvider());
            }
        }
    }

    private void extractDataFromElement(Map<String, Object> resultContainer,
                                        BaseElement element,
                                        ElementData contextData) {
        var elementDataObject = contextData
                .getOrDefault(element.getId(), new ElementDataObject(element));

        if (!elementDataObject.getIsVisible()) {
            return;
        }

        var resolvedElement = elementDataObject
                .getComputedOverrideOrDefault(element);

        Consumer<BaseElement> extractChildData = (e) -> extractDataFromElement(resultContainer, e, contextData);

        switch (resolvedElement) {
            case BaseFormElement formElement -> {
                switch (formElement) {
                    case GroupLayoutElement groupLayout -> groupLayout.getChildren().forEach(extractChildData);
                    case ReplicatingContainerLayoutElement replicatingContainerLayout ->
                            extractReplicatingContainer(resultContainer, replicatingContainerLayout, elementDataObject);
                    case FileUploadInputElement fileUploadField ->
                            extractFileUploadField(resultContainer, fileUploadField, elementDataObject);
                    case BaseInputElement<?> baseInputElement ->
                            extractBaseInput(resultContainer, baseInputElement, elementDataObject);
                    default -> {
                        // Do nothing
                    }
                }
            }
            case FormLayoutElement rootElement -> rootElement.getChildren().forEach(extractChildData);
            case GenericStepElement stepElement -> stepElement.getChildren().forEach(extractChildData);
            case null, default -> {
                // Do nothing
            }
        }
    }

    private void extractReplicatingContainer(Map<String, Object> resultContainer,
                                             ReplicatingContainerLayoutElement element,
                                             ElementDataObject containerDataObject) {
        var rawDataSets = containerDataObject.getValue();

        if (!(rawDataSets instanceof Collection<?> dataSets)) {
            return;
        }

        var elementDestinationKey = element.getDestinationKey() != null ? element.getDestinationKey() : element.getId();
        if (destinationSkipKey.equals(elementDestinationKey)) {
            return;
        }

        var elementDataConverter = new ElementDataConverter();
        var extractedChildDataList = new ArrayList<Map<String, Object>>();

        for (var dataSetObj : dataSets) {
            ElementData childDataSet;
            if (dataSetObj instanceof ElementData elementData) {
                childDataSet = elementData;
            } else if (dataSetObj instanceof Map<?, ?> mapObj) {
                childDataSet = elementDataConverter.convertObjectToEntityAttribute(mapObj);
            } else {
                continue;
            }

            var childResult = new HashMap<String, Object>();
            for (var child : element.getChildren()) {
                extractDataFromElement(childResult, child, childDataSet);
            }
            extractedChildDataList.add(childResult);
        }

        insertValue(resultContainer, elementDestinationKey, extractedChildDataList);
    }

    private void extractFileUploadField(Map<String, Object> resultContainer,
                                        FileUploadInputElement element,
                                        ElementDataObject elementDataObject) {
        if (!includeAttachments()) {
            return;
        }

        var valuesObj = elementDataObject.getValue();

        if (!(valuesObj instanceof Collection<?> values)) {
            return;
        }

        var elementDestinationKey = element.getDestinationKey() != null ? element.getDestinationKey() : element.getId();
        if (destinationSkipKey.equals(elementDestinationKey)) {
            return;
        }

        var annotatedValues = new LinkedList<Object>();
        for (var fileUploadValueItem : values) {
            if (fileUploadValueItem instanceof Map<?, ?> fileUploadValueItemMap) {
                var mutableItem = castStringObjectMap(fileUploadValueItemMap);
                var fileNameObj = mutableItem.get("name");
                if (fileNameObj != null && attachmentBytes.containsKey(fileNameObj)) {
                    var bytes = attachmentBytes.get(fileNameObj);
                    var attachmentBase64 = loadBytesAsBase64(bytes);
                    mutableItem.put("base64", attachmentBase64);
                }
                annotatedValues.add(mutableItem);
            } else {
                annotatedValues.add(fileUploadValueItem);
            }
        }

        insertValue(resultContainer, elementDestinationKey, annotatedValues);
    }

    private void extractBaseInput(Map<String, Object> resultContainer,
                                  BaseInputElement<?> element,
                                  ElementDataObject elementDataObject) {
        var value = elementDataObject.getValue();

        if (value == null) {
            return;
        }

        var elementDestinationKey = element.getDestinationKey() != null ? element.getDestinationKey() : element.getId();
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

    private static Map<String, Object> castStringObjectMap(Map<?, ?> raw) {
        var result = new HashMap<String, Object>();
        for (var entry : raw.entrySet()) {
            if (entry.getKey() instanceof String key) {
                result.put(key, entry.getValue());
            }
        }
        return result;
    }
}
