package de.aivot.GoverBackend.ozgCloud.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.elements.models.BaseElement;
import de.aivot.GoverBackend.elements.models.RootElement;
import de.aivot.GoverBackend.elements.models.form.input.*;
import de.aivot.GoverBackend.elements.models.form.layout.GroupLayout;
import de.aivot.GoverBackend.elements.models.form.layout.ReplicatingContainerLayout;
import de.aivot.GoverBackend.elements.models.steps.StepElement;
import de.aivot.GoverBackend.enums.TableColumnDataType;
import de.aivot.GoverBackend.form.models.FormState;
import de.aivot.GoverBackend.ozgCloud.models.OZGCloudControlData;
import de.aivot.GoverBackend.ozgCloud.models.OZGCloudFormDataItem;
import de.aivot.GoverBackend.ozgCloud.models.OZGCloudPayload;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OZGCloudDestinationService {
    private static final String FORM_FIELD_PAYLOAD = "formData";
    private static final String FORM_FIELD_REPRESENTATION = "representation";
    private static final String FORM_FIELD_ATTACHMENT = "attachment";

    private final RestClient httpClient;

    public OZGCloudDestinationService() {
        this.httpClient = RestClient
                .builder()
                .build();
    }

    public void send(
            @Nonnull String destinationUrl,
            @Nonnull OZGCloudControlData controlData,
            @Nonnull BaseElement rootElement,
            @Nonnull Map<String, Object> elementData,
            @Nonnull Resource representation,
            @Nonnull List<Resource> attachments,
            @Nonnull FormState formState
    ) {
        var destinationUri = URI.create(destinationUrl);

        var formData = new OZGCloudDataFormatService()
                .buildFormData(rootElement, elementData, null, formState);

        var payload = new OZGCloudPayload(
                controlData,
                formData
        );

        try {
            submit(destinationUri, payload, representation, attachments);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void submit(
            @Nonnull URI destinationUri,
            @Nonnull OZGCloudPayload payload,
            @Nonnull Resource representation,
            @Nonnull List<Resource> attachments
    ) throws IOException {
        var payloadJSON = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .writeValueAsString(payload);

        var body = new MultipartBodyPublisher()
                .addPart(FORM_FIELD_PAYLOAD, payloadJSON)
                .addPart(FORM_FIELD_REPRESENTATION, representation);

        for (var attachment : attachments) {
            body.addPart(FORM_FIELD_ATTACHMENT, attachment);
        }

        var responseBody = httpClient
                .post()
                .uri(destinationUri)
                .body(body.build())
                .retrieve()
                .body(byte[].class);

        if (responseBody == null) {
            throw new RuntimeException("Received null response body from OZGCloud destination");
        }
    }

    public static class MultipartBodyPublisher {
        private final MultiValueMap<String, Resource> parts = new LinkedMultiValueMap<>();

        public MultipartBodyPublisher addPart(String name, String value) {
            var res = new ByteArrayResource(value.getBytes(StandardCharsets.UTF_8));
            parts.add(name, res);
            return this;
        }

        public MultipartBodyPublisher addPart(String name, Resource resource) {
            parts.add(name, resource);
            return this;
        }

        public Object build() {
            return parts;
        }
    }
}
