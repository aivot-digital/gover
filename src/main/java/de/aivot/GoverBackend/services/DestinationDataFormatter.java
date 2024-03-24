package de.aivot.GoverBackend.services;

import de.aivot.GoverBackend.data.SpecialCustomerInputKeys;
import de.aivot.GoverBackend.enums.Idp;
import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.models.elements.RootElement;
import de.aivot.GoverBackend.models.elements.form.BaseFormElement;
import de.aivot.GoverBackend.models.elements.form.BaseInputElement;
import de.aivot.GoverBackend.models.elements.form.input.FileUploadField;
import de.aivot.GoverBackend.models.elements.form.layout.GroupLayout;
import de.aivot.GoverBackend.models.elements.form.layout.ReplicatingContainerLayout;
import de.aivot.GoverBackend.models.entities.Form;
import de.aivot.GoverBackend.models.entities.Submission;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class DestinationDataFormatter {
    private final Map<String, Object> data;
    private static final String destinationSkipKey = "#";

    private DestinationDataFormatter() {
        this.data = new HashMap<>();
    }

    public static DestinationDataFormatter create() {
        return new DestinationDataFormatter();
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

    public Map<String, Object> formatDestinationData(Form form, RootElement rootElement, Submission submission, Map<String, Object> customerInput, Path pdfPath, Map<String, Path> attachmentPaths) {
        createFormData(form);
        createMetadata(submission);
        createAuthenticationData(customerInput);

        Map<String, Object> customerData = new HashMap<>();
        for (var step : rootElement.getChildren()) {
            formatChildrenData(customerData, step.getChildren(), customerInput, attachmentPaths, null);
        }
        this.data.put("data", customerData);

        try {
            this.data.put("_pdf", loadFileAsBase64(pdfPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return this.data;
    }

    private void createFormData(Form form) {
        insertValue("form.id", form.getId());
        insertValue("form.name", form.getTitle());
        insertValue("form.slug", form.getSlug());
        insertValue("form.version", form.getVersion());
        insertValue("form.headline", form.getApplicationTitle());
        insertValue("form.managing_department_id", form.getManagingDepartmentId());
        insertValue("form.responsible_department_id", form.getResponsibleDepartmentId());
        insertValue("form.developing_department_id", form.getDevelopingDepartmentId());
    }

    private void createMetadata(Submission submission) {
        insertValue("metadata.is_test_submission", submission.getIsTestSubmission());
        insertValue("metadata.submitted", submission.getCreated().format(DateTimeFormatter.ISO_DATE_TIME));
        insertValue("metadata.user_rating", submission.getReviewScore());
    }

    private void createAuthenticationData(Map<String, Object> customerInput) {

        var rawIdpData = customerInput.get(SpecialCustomerInputKeys.IdCustomerInputKey);
        if (rawIdpData instanceof Map<?, ?> idpData) {
            var idpRaw = idpData.get("idp");
            if (idpRaw instanceof String sIdp) {
                insertValue("authentication.is_authenticated", true);
                insertValue("authentication.identity_provider", sIdp);

                var authData = new HashMap<String, Object>();
                var rawUserInfo = idpData.get(SpecialCustomerInputKeys.UserInfoKey);
                if (rawUserInfo instanceof Map<?, ?> userInfo) {
                    var typedUserInfo = (Map<String, Object>) userInfo;
                    authData.putAll(typedUserInfo);
                }
                insertValue("authentication.data", authData);
            } else {
                insertValue("authentication.is_authenticated", false);
            }
        } else {
            insertValue("authentication.is_authenticated", false);
        }
    }

    private void formatChildrenData(Map<String, Object> data, Collection<BaseFormElement> children, Map<String, Object> customerInput, Map<String, Path> attachmentPaths, String idPrefix) {
        for (var child : children) {
            var childId = (idPrefix != null ? idPrefix : "") + child.getId();

            // Format a replicating container
            if (child instanceof ReplicatingContainerLayout replicatingContainerLayout) {
                formatReplicationContainerData(data, customerInput, attachmentPaths, replicatingContainerLayout, childId);
            }

            // Format a group layout
            else if (child instanceof GroupLayout groupLayout) {
                formatChildrenData(data, groupLayout.getChildren(), customerInput, attachmentPaths, idPrefix);
            }

            // Format a file upload field
            else if (child instanceof FileUploadField fileUploadField) {
                if (!customerInput.containsKey(childId)) {
                    continue;
                }

                var destinationKey = fileUploadField.getDestinationKey() != null ? fileUploadField.getDestinationKey() : childId;

                if (destinationSkipKey.equals(destinationKey)) {
                    continue;
                }

                Object fileUploadValue = customerInput.get(childId);

                if (fileUploadValue instanceof Collection<?> fileUploadValueCollection) {
                    for (var fileUploadValueItem : fileUploadValueCollection) {
                        if (fileUploadValueItem instanceof Map<?, ?> fileUploadValueItemMap) {
                            var fileName = fileUploadValueItemMap.get("name");

                            if (fileName != null && attachmentPaths.containsKey(fileName)) {
                                var attachmentPath = attachmentPaths.get(fileName);
                                try {
                                    var attachmentBase64 = loadFileAsBase64(attachmentPath);
                                    ((Map<String, Object>) fileUploadValueItemMap).put("base64", attachmentBase64);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                }

                insertValue(data, destinationKey, fileUploadValue);
            }

            // Format any other input field
            else if (child instanceof BaseInputElement<?> inputElement) {
                var destinationKey = inputElement.getDestinationKey() != null ? inputElement.getDestinationKey() : childId;

                if (destinationSkipKey.equals(destinationKey)) {
                    continue;
                }

                insertValue(data, destinationKey, customerInput.get(childId));
            }
        }
    }

    private void formatReplicationContainerData(Map<String, Object> data, Map<String, Object> customerInput, Map<String, Path> attachmentPaths, ReplicatingContainerLayout replicatingContainerLayout, String childId) {
        if (!customerInput.containsKey(childId)) {
            return;
        }

        var destinationKey = replicatingContainerLayout.getDestinationKey() != null ? replicatingContainerLayout.getDestinationKey() : childId;

        if (destinationSkipKey.equals(destinationKey)) {
            return;
        }

        List<Map<String, Object>> repData = new ArrayList<>();

        Collection<String> repChildIds = (Collection<String>) customerInput.get(childId);

        for (var repChildId : repChildIds) {
            var repDataItem = new HashMap<String, Object>();
            formatChildrenData(repDataItem, replicatingContainerLayout.getChildren(), customerInput, attachmentPaths, childId + "_" + repChildId + "_");
            repData.add(repDataItem);
        }

        insertValue(data, destinationKey, repData);
    }

    private String loadFileAsBase64(Path path) throws IOException {
        return org.apache.commons.codec.binary.Base64.encodeBase64String(Files.readAllBytes(path));
    }
}
