package de.aivot.gover.backend.payment.services;

import de.aivot.gover.backend.elements.models.elements.form.input.PaymentConfigElementValue;
import de.aivot.gover.backend.elements.models.elements.form.input.PaymentConfigElementValueItem;
import de.aivot.gover.backend.elements.models.elements.form.input.PaymentConfigElementValueRequestorMapping;
import de.aivot.gover.backend.enums.XBezahldienstGender;
import de.aivot.gover.backend.javascript.models.JavascriptCode;
import de.aivot.gover.backend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.gover.backend.nocode.models.NoCodeStaticValue;
import de.aivot.gover.backend.nocode.services.NoCodeEvaluationService;
import de.aivot.gover.backend.payment.exceptions.PaymentException;
import de.aivot.gover.backend.process.models.ProcessExecutionData;
import de.aivot.gover.backend.process.services.TemplateRenderService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentPayloadCreationServiceTest {
    @Test
    void shouldCreatePaymentRequestFromFixedItemAndRequestorMapping() throws PaymentException {
        var processData = new ProcessExecutionData().addProcessData(Map.of(
                "caseNumber", "AZ-1",
                "name", "Ada",
                "requestor", Map.ofEntries(
                        Map.entry("lastName", "Lovelace"),
                        Map.entry("firstName", "Ada"),
                        Map.entry("gender", "F"),
                        Map.entry("isOrganization", false),
                        Map.entry("organizationName", "Ignored Ltd"),
                        Map.entry("street", "Analytical Engine Road"),
                        Map.entry("houseNumber", "1"),
                        Map.entry("addressLine", "Suite 2"),
                        Map.entry("postalCode", "12345"),
                        Map.entry("city", "London"),
                        Map.entry("country", "gb")
                )
        ));
        var config = new PaymentConfigElementValue(
                null,
                "PAY {{ $.caseNumber }}",
                "Fee for {{ $.name }}",
                true,
                new PaymentConfigElementValueRequestorMapping(
                        PaymentConfigElementValueRequestorMapping.RequestorSourceType.ProcessDataKey,
                        "requestor.lastName",
                        "requestor.firstName",
                        "requestor.gender",
                        "requestor.isOrganization",
                        "requestor.organizationName",
                        "requestor.street",
                        "requestor.houseNumber",
                        "requestor.addressLine",
                        "requestor.postalCode",
                        "requestor.city",
                        "requestor.country"
                ),
                List.of(new PaymentConfigElementValueItem(
                        PaymentConfigElementValueItem.IdType.Predefined,
                        "fee-1",
                        "Description {{ $.name }}",
                        "REF-{{ $.caseNumber }}",
                        PaymentConfigElementValueItem.CostType.FixedCosts,
                        new BigDecimal("10.00"),
                        null,
                        null,
                        null,
                        PaymentConfigElementValueItem.QuantityType.FixedQuantity,
                        2L,
                        null,
                        null,
                        null,
                        new BigDecimal("19.00"),
                        Map.of("case", "{{ $.caseNumber }}")
                )),
                null,
                null
        );

        var request = createService().createRequest(config, null, processData).orElseThrow();

        assertEquals("PAY AZ-1", request.getPurpose());
        assertEquals("Fee for Ada", request.getDescription());
        assertEquals(new BigDecimal("23.80"), request.getTotal());
        assertEquals(false, request.getRequestor().getOrganization());
        assertEquals("Lovelace", request.getRequestor().getLastName());
        assertEquals(XBezahldienstGender.FEMALE, request.getRequestor().getGender());
        assertEquals("London", request.getRequestor().getAddress().getCity());
        assertEquals("GB", request.getRequestor().getAddress().getCountry());

        var item = request.getPaymentItems().getFirst();
        assertEquals("fee-1", item.getId());
        assertEquals("REF-AZ-1", item.getReference());
        assertEquals("Description Ada", item.getDescription());
        assertEquals(2L, item.getQuantity());
        assertEquals(new BigDecimal("10.00"), item.getNetPrice());
        assertEquals(new BigDecimal("23.80"), item.getTotalPrice());
        assertEquals("AZ-1", item.getBookingData().getFirst().value());
    }

    @Test
    void shouldCreateFixedPersonRequestor() throws PaymentException {
        var processData = new ProcessExecutionData().addProcessData(Map.of(
                "person", Map.ofEntries(
                        Map.entry("lastName", "Doe"),
                        Map.entry("firstName", "Jane"),
                        Map.entry("gender", "D"),
                        Map.entry("isOrganization", true),
                        Map.entry("organizationName", "Ignored GmbH"),
                        Map.entry("street", "Main Street"),
                        Map.entry("houseNumber", "1"),
                        Map.entry("addressLine", "Floor 2"),
                        Map.entry("postalCode", "12345"),
                        Map.entry("city", "Berlin"),
                        Map.entry("country", "de")
                )
        ));

        var request = createService().createRequest(requestorConfig(new PaymentConfigElementValueRequestorMapping(
                PaymentConfigElementValueRequestorMapping.RequestorSourceType.FixPerson,
                "person.lastName",
                "person.firstName",
                "person.gender",
                "person.isOrganization",
                "person.organizationName",
                "person.street",
                "person.houseNumber",
                "person.addressLine",
                "person.postalCode",
                "person.city",
                "person.country"
        )), null, processData).orElseThrow();

        var requestor = request.getRequestor();
        assertEquals(false, requestor.getOrganization());
        assertEquals("Doe", requestor.getLastName());
        assertEquals("Jane", requestor.getFirstName());
        assertEquals(XBezahldienstGender.DIVERSE, requestor.getGender());
        assertNull(requestor.getOrganizationName());
        assertEquals("Berlin", requestor.getAddress().getCity());
    }

    @Test
    void shouldCreateFixedPersonRequestorWithoutOptionalFields() throws PaymentException {
        var request = createService()
                .createRequest(
                        requestorConfig(emptyRequestorMapping(PaymentConfigElementValueRequestorMapping.RequestorSourceType.FixPerson, null)),
                        null,
                        new ProcessExecutionData()
                )
                .orElseThrow();

        var requestor = request.getRequestor();
        assertEquals(false, requestor.getOrganization());
        assertNull(requestor.getLastName());
        assertNull(requestor.getFirstName());
        assertNull(requestor.getGender());
        assertNull(requestor.getOrganizationName());
        assertNull(requestor.getAddress());
    }

    @Test
    void shouldCreateFixedOrganizationRequestor() throws PaymentException {
        var processData = new ProcessExecutionData().addProcessData(Map.of(
                "company", Map.of(
                        "lastName", "Ignored",
                        "firstName", "Ignored",
                        "organizationName", "Acme GmbH",
                        "street", "Market Street",
                        "houseNumber", "2",
                        "addressLine", "Building A",
                        "postalCode", "23456",
                        "city", "Hamburg",
                        "country", "de"
                )
        ));

        var request = createService().createRequest(requestorConfig(new PaymentConfigElementValueRequestorMapping(
                PaymentConfigElementValueRequestorMapping.RequestorSourceType.FixOrg,
                "company.lastName",
                "company.firstName",
                null,
                null,
                "company.organizationName",
                "company.street",
                "company.houseNumber",
                "company.addressLine",
                "company.postalCode",
                "company.city",
                "company.country"
        )), null, processData).orElseThrow();

        var requestor = request.getRequestor();
        assertEquals(true, requestor.getOrganization());
        assertEquals("Acme GmbH", requestor.getOrganizationName());
        assertNull(requestor.getLastName());
        assertNull(requestor.getFirstName());
        assertNull(requestor.getGender());
        assertEquals("Hamburg", requestor.getAddress().getCity());
    }

    @Test
    void shouldCreateFixedOrganizationRequestorWithoutOptionalFields() throws PaymentException {
        var request = createService()
                .createRequest(
                        requestorConfig(emptyRequestorMapping(PaymentConfigElementValueRequestorMapping.RequestorSourceType.FixOrg, null)),
                        null,
                        new ProcessExecutionData()
                )
                .orElseThrow();

        var requestor = request.getRequestor();
        assertEquals(true, requestor.getOrganization());
        assertNull(requestor.getOrganizationName());
        assertNull(requestor.getLastName());
        assertNull(requestor.getFirstName());
        assertNull(requestor.getGender());
        assertNull(requestor.getAddress());
    }

    @Test
    void shouldCreateDynamicOrganizationRequestor() throws PaymentException {
        var processData = new ProcessExecutionData().addProcessData(Map.of(
                "requestor", Map.ofEntries(
                        Map.entry("isOrganization", true),
                        Map.entry("lastName", "Ignored"),
                        Map.entry("firstName", "Ignored"),
                        Map.entry("gender", "M"),
                        Map.entry("organizationName", "Dynamic GmbH"),
                        Map.entry("street", "Dynamic Street"),
                        Map.entry("houseNumber", "3"),
                        Map.entry("addressLine", "Unit 4"),
                        Map.entry("postalCode", "34567"),
                        Map.entry("city", "Munich"),
                        Map.entry("country", "de")
                )
        ));

        var request = createService().createRequest(requestorConfig(new PaymentConfigElementValueRequestorMapping(
                PaymentConfigElementValueRequestorMapping.RequestorSourceType.ProcessDataKey,
                "requestor.lastName",
                "requestor.firstName",
                "requestor.gender",
                "requestor.isOrganization",
                "requestor.organizationName",
                "requestor.street",
                "requestor.houseNumber",
                "requestor.addressLine",
                "requestor.postalCode",
                "requestor.city",
                "requestor.country"
        )), null, processData).orElseThrow();

        var requestor = request.getRequestor();
        assertEquals(true, requestor.getOrganization());
        assertEquals("Dynamic GmbH", requestor.getOrganizationName());
        assertNull(requestor.getLastName());
        assertNull(requestor.getFirstName());
        assertEquals("Munich", requestor.getAddress().getCity());
    }

    @Test
    void shouldCreateDynamicRequestorWithOnlyOrganizationFlag() throws PaymentException {
        var processData = new ProcessExecutionData().addProcessData(Map.of(
                "requestor", Map.of("isOrganization", false)
        ));

        var request = createService()
                .createRequest(
                        requestorConfig(emptyRequestorMapping(PaymentConfigElementValueRequestorMapping.RequestorSourceType.ProcessDataKey, "requestor.isOrganization")),
                        null,
                        processData
                )
                .orElseThrow();

        var requestor = request.getRequestor();
        assertEquals(false, requestor.getOrganization());
        assertNull(requestor.getLastName());
        assertNull(requestor.getFirstName());
        assertNull(requestor.getGender());
        assertNull(requestor.getOrganizationName());
        assertNull(requestor.getAddress());
    }

    @Test
    void shouldRejectDynamicRequestorWithoutOrganizationFlag() {
        var processData = new ProcessExecutionData().addProcessData(Map.of(
                "requestor", Map.of(
                        "lastName", "Lovelace",
                        "firstName", "Ada"
                )
        ));

        assertThrows(PaymentException.class, () -> createService().createRequest(requestorConfig(new PaymentConfigElementValueRequestorMapping(
                PaymentConfigElementValueRequestorMapping.RequestorSourceType.ProcessDataKey,
                "requestor.lastName",
                "requestor.firstName",
                "requestor.gender",
                "requestor.isOrganization",
                "requestor.organizationName",
                "requestor.street",
                "requestor.houseNumber",
                "requestor.addressLine",
                "requestor.postalCode",
                "requestor.city",
                "requestor.country"
        )), null, processData));
    }

    @Test
    void shouldEvaluateVariableCostsAndQuantity() throws PaymentException {
        var processData = new ProcessExecutionData().addProcessData(Map.of("count", 2));
        var config = new PaymentConfigElementValue(
                null,
                "Purpose",
                "Description",
                false,
                null,
                List.of(new PaymentConfigElementValueItem(
                        PaymentConfigElementValueItem.IdType.AutoGeneratedUUID,
                        null,
                        "Variable",
                        "VAR",
                        PaymentConfigElementValueItem.CostType.VariableCosts,
                        null,
                        PaymentConfigElementValueItem.VariableValueCalculationType.NoCode,
                        new NoCodeStaticValue(new BigDecimal("7.50")),
                        null,
                        PaymentConfigElementValueItem.QuantityType.VariableQuantity,
                        null,
                        PaymentConfigElementValueItem.VariableValueCalculationType.LowCode,
                        null,
                        JavascriptCode.of("$.count + 1"),
                        BigDecimal.ZERO,
                        null
                )),
                null,
                null
        );

        var request = createService().createRequest(config, null, processData).orElseThrow();

        assertEquals(1, request.getPaymentItems().size());
        assertEquals(3L, request.getPaymentItems().getFirst().getQuantity());
        assertEquals(new BigDecimal("22.50"), request.getTotal());
    }

    @Test
    void shouldCalculateTotalWithPerItemTaxRounding() throws PaymentException {
        var config = new PaymentConfigElementValue(
                null,
                "Purpose",
                "Description",
                false,
                null,
                List.of(new PaymentConfigElementValueItem(
                        PaymentConfigElementValueItem.IdType.Predefined,
                        "rounding",
                        "Rounding",
                        "ROUND",
                        PaymentConfigElementValueItem.CostType.FixedCosts,
                        new BigDecimal("0.01"),
                        null,
                        null,
                        null,
                        PaymentConfigElementValueItem.QuantityType.FixedQuantity,
                        3L,
                        null,
                        null,
                        null,
                        new BigDecimal("19.00"),
                        null
                )),
                null,
                null
        );

        var request = createService().createRequest(config, null, new ProcessExecutionData()).orElseThrow();

        assertEquals(new BigDecimal("0.03"), request.getPaymentItems().getFirst().getTotalPrice());
        assertEquals(new BigDecimal("0.03"), request.getTotal());
    }

    @Test
    void shouldRejectRequestsWithoutPayableItems() {
        var config = new PaymentConfigElementValue(
                null,
                "Purpose",
                "Description",
                false,
                null,
                List.of(new PaymentConfigElementValueItem(
                        PaymentConfigElementValueItem.IdType.Predefined,
                        "zero",
                        "Zero",
                        "ZERO",
                        PaymentConfigElementValueItem.CostType.FixedCosts,
                        BigDecimal.TEN,
                        null,
                        null,
                        null,
                        PaymentConfigElementValueItem.QuantityType.FixedQuantity,
                        0L,
                        null,
                        null,
                        null,
                        BigDecimal.ZERO,
                        null
                )),
                null,
                null
        );

        assertThrows(
                PaymentException.class,
                () -> createService().createRequest(config, null, new ProcessExecutionData())
        );
    }

    private static PaymentConfigElementValueRequestorMapping emptyRequestorMapping(
            PaymentConfigElementValueRequestorMapping.RequestorSourceType sourceType,
            String isOrganizationDestinationKey
    ) {
        return new PaymentConfigElementValueRequestorMapping(
                sourceType,
                null,
                null,
                null,
                isOrganizationDestinationKey,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private static PaymentConfigElementValue requestorConfig(PaymentConfigElementValueRequestorMapping requestorMapping) {
        return new PaymentConfigElementValue(
                null,
                "Purpose",
                "Description",
                true,
                requestorMapping,
                List.of(new PaymentConfigElementValueItem(
                        PaymentConfigElementValueItem.IdType.Predefined,
                        "fee-1",
                        "Fee",
                        "FEE",
                        PaymentConfigElementValueItem.CostType.FixedCosts,
                        BigDecimal.TEN,
                        null,
                        null,
                        null,
                        PaymentConfigElementValueItem.QuantityType.FixedQuantity,
                        1L,
                        null,
                        null,
                        null,
                        BigDecimal.ZERO,
                        null
                )),
                null,
                null
        );
    }

    private static PaymentPayloadCreationService createService() {
        var javascriptEngineFactoryService = new JavascriptEngineFactoryService(List.of());
        return new PaymentPayloadCreationService(
                new TemplateRenderService(javascriptEngineFactoryService),
                new NoCodeEvaluationService(List.of()),
                javascriptEngineFactoryService
        );
    }
}
