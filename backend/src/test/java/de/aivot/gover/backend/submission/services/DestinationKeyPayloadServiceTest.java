package de.aivot.gover.backend.submission.services;

import de.aivot.gover.backend.elements.models.ComputedElementState;
import de.aivot.gover.backend.elements.models.ComputedElementStates;
import de.aivot.gover.backend.elements.models.EffectiveElementValues;
import de.aivot.gover.backend.elements.models.elements.BaseElement;
import de.aivot.gover.backend.elements.models.elements.BaseFormElement;
import de.aivot.gover.backend.elements.models.elements.form.input.DateInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.DateTimeInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.RangeInputElementValue;
import de.aivot.gover.backend.elements.models.elements.form.input.TableInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.TimeInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.TimeRangeInputElement;
import de.aivot.gover.backend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.gover.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.gover.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.gover.backend.elements.models.elements.steps.BaseStepElement;
import de.aivot.gover.backend.elements.models.elements.steps.GenericStepElement;
import de.aivot.gover.backend.utils.ApplicationTimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DestinationKeyPayloadServiceTest {
    private final ElementDataTransformService service = new ElementDataTransformService();
    private ZoneId originalZoneId;

    @BeforeEach
    void configureApplicationTimeZone() {
        originalZoneId = ApplicationTimeZone.getZoneId();
        ApplicationTimeZone.configure(ZoneId.of("Europe/Berlin"));
    }

    @AfterEach
    void restoreApplicationTimeZone() {
        ApplicationTimeZone.configure(originalZoneId);
    }

    @Test
    void shouldBuildPayloadFromDestinationKeys() {
        var firstName = new TextInputElement();
        firstName.setId("firstName");
        firstName.setDestinationKey("person.first_name");

        var street = new TextInputElement();
        street.setId("street");
        street.setDestinationKey("person.address.street");

        var ignored = new TextInputElement();
        ignored.setId("ignored");

        var hidden = new TextInputElement();
        hidden.setId("hidden");
        hidden.setDestinationKey("person.last_name");

        var group = new GroupLayoutElement();
        group.setChildren(new LinkedList<>(List.of(firstName, street, ignored, hidden)));

        var effectiveValues = new EffectiveElementValues();
        effectiveValues.put("firstName", "Ada");
        effectiveValues.put("street", "Main Street 1");
        effectiveValues.put("ignored", "should-not-be-exported");

        var payload = service.buildPayload(createRoot(group), effectiveValues);

        assertEquals(
                Map.of(
                        "person", Map.of(
                                "first_name", "Ada",
                                "address", Map.of("street", "Main Street 1")
                        )
                ),
                payload
        );
    }

    @Test
    void shouldSerializeSemanticTemporalValuesWithoutElementSpecificProjection() {
        var month = new DateInputElement();
        month.setId("month");
        month.setDestinationKey("period.month");

        var time = new TimeInputElement();
        time.setId("time");
        time.setDestinationKey("period.time");

        var dateTime = new DateTimeInputElement();
        dateTime.setId("dateTime");
        dateTime.setDestinationKey("period.date_time");

        var timeRange = new TimeRangeInputElement();
        timeRange.setId("timeRange");
        timeRange.setDestinationKey("period.time_range");

        var group = new GroupLayoutElement();
        group.setChildren(new LinkedList<>(List.of(month, time, dateTime, timeRange)));

        var effectiveValues = new EffectiveElementValues();
        effectiveValues.put("month", YearMonth.of(2026, 7));
        effectiveValues.put("time", LocalTime.of(9, 30));
        effectiveValues.put("dateTime", Instant.parse("2026-07-29T07:00:00Z"));
        effectiveValues.put(
                "timeRange",
                new RangeInputElementValue<>(
                        LocalTime.of(9, 30),
                        LocalTime.of(10, 45, 15)
                )
        );

        var payload = service.buildPayload(createRoot(group), effectiveValues);

        assertEquals(
                Map.of(
                        "period", Map.of(
                                "month", "2026-07",
                                "time", "09:30:00",
                                "date_time", "2026-07-29T09:00:00+02:00",
                                "time_range", Map.of(
                                        "start", "09:30:00",
                                        "end", "10:45:15"
                                )
                        )
                ),
                payload
        );
    }

    @Test
    void shouldUseRuntimeOverrideDestinationKeyWhenBuildingPayload() {
        var table = new TableInputElement();
        table.setId("table");

        var tableOverride = new TableInputElement();
        tableOverride.setId("table");
        tableOverride.setDestinationKey("tabellenKeyViaCodeGesetzt");

        var elementStates = new ComputedElementStates();
        elementStates.put("table", new ComputedElementState().setOverride(tableOverride));

        var tableRows = List.of(
                Map.of("name", "Ada"),
                Map.of("name", "Grace")
        );
        var effectiveValues = new EffectiveElementValues();
        effectiveValues.put("table", tableRows);

        var payload = service.buildPayload(createRoot(table), effectiveValues, elementStates);

        assertEquals(
                Map.of("tabellenKeyViaCodeGesetzt", tableRows),
                payload
        );
    }

    @Test
    void shouldUseRuntimeOverrideDestinationKeysInReplicatingContainerRows() {
        var firstName = new TextInputElement();
        firstName.setId("rowFirstName");

        var firstNameOverride = new TextInputElement();
        firstNameOverride.setId("rowFirstName");
        firstNameOverride.setDestinationKey("first_name");

        var people = new ReplicatingContainerLayoutElement();
        people.setId("people");
        people.setDestinationKey("people");
        people.setChildren(new LinkedList<>(List.of(firstName)));

        var firstPerson = new EffectiveElementValues();
        firstPerson.put("rowFirstName", "Ada");

        var secondPerson = new EffectiveElementValues();
        secondPerson.put("rowFirstName", "Grace");

        var effectiveValues = new EffectiveElementValues();
        effectiveValues.put("people", List.of(firstPerson, secondPerson));

        var peopleState = new ComputedElementState().setSubStates(List.of(
                elementStatesWithOverride("rowFirstName", firstNameOverride),
                elementStatesWithOverride("rowFirstName", firstNameOverride)
        ));
        var elementStates = new ComputedElementStates();
        elementStates.put("people", peopleState);

        var payload = service.buildPayload(createRoot(people), effectiveValues, elementStates);

        assertEquals(
                Map.of(
                        "people", List.of(
                                Map.of("first_name", "Ada"),
                                Map.of("first_name", "Grace")
                        )
                ),
                payload
        );
    }

    @Test
    void shouldBuildReplicatingContainerPayloadFromDestinationKeys() {
        var firstName = new TextInputElement();
        firstName.setId("rowFirstName");
        firstName.setDestinationKey("first_name");

        var street = new TextInputElement();
        street.setId("rowStreet");
        street.setDestinationKey("address.street");

        var people = new ReplicatingContainerLayoutElement();
        people.setId("people");
        people.setDestinationKey("payload.people");
        people.setChildren(new LinkedList<>(List.of(firstName, street)));

        var firstPerson = new EffectiveElementValues();
        firstPerson.put("rowFirstName", "Ada");
        firstPerson.put("rowStreet", "Main Street 1");

        var secondPerson = new EffectiveElementValues();
        secondPerson.put("rowFirstName", "Grace");
        secondPerson.put("rowStreet", "Side Alley 2");

        var effectiveValues = new EffectiveElementValues();
        effectiveValues.put("people", List.of(firstPerson, secondPerson));

        var payload = service.buildPayload(createRoot(people), effectiveValues);

        assertEquals(
                Map.of(
                        "payload", Map.of(
                                "people", List.of(
                                        Map.of(
                                                "first_name", "Ada",
                                                "address", Map.of("street", "Main Street 1")
                                        ),
                                        Map.of(
                                                "first_name", "Grace",
                                                "address", Map.of("street", "Side Alley 2")
                                        )
                                )
                        )
                ),
                payload
        );
    }

    @Test
    void shouldPatchExistingReplicatingContainerRowsWithoutDroppingSiblingFields() {
        var firstName = new TextInputElement();
        firstName.setId("rowFirstName");
        firstName.setDestinationKey("first_name");

        var people = new ReplicatingContainerLayoutElement();
        people.setId("people");
        people.setDestinationKey("payload.people");
        people.setChildren(new LinkedList<>(List.of(firstName)));

        var firstPerson = new EffectiveElementValues();
        firstPerson.put("rowFirstName", "Ada Updated");

        var secondPerson = new EffectiveElementValues();
        secondPerson.put("rowFirstName", "Grace Updated");

        var effectiveValues = new EffectiveElementValues();
        effectiveValues.put("people", List.of(firstPerson, secondPerson));

        var payload = service.buildPayload(
                createRoot(people),
                effectiveValues,
                Map.of(
                        "payload", Map.of(
                                "people", List.of(
                                        Map.of(
                                                "first_name", "Ada",
                                                "age", 33,
                                                "address", Map.of(
                                                        "street", "Main Street 1",
                                                        "city", "Berlin"
                                                )
                                        ),
                                        Map.of(
                                                "first_name", "Grace",
                                                "age", 41,
                                                "address", Map.of(
                                                        "street", "Side Alley 2",
                                                        "city", "Hamburg"
                                                )
                                        )
                                ),
                                "untouched", "value"
                        )
                )
        );

        assertEquals(
                Map.of(
                        "payload", Map.of(
                                "people", List.of(
                                        Map.of(
                                                "first_name", "Ada Updated",
                                                "age", 33,
                                                "address", Map.of(
                                                        "street", "Main Street 1",
                                                        "city", "Berlin"
                                                )
                                        ),
                                        Map.of(
                                                "first_name", "Grace Updated",
                                                "age", 41,
                                                "address", Map.of(
                                                        "street", "Side Alley 2",
                                                        "city", "Hamburg"
                                                )
                                        )
                                ),
                                "untouched", "value"
                        )
                ),
                payload
        );
    }

    @Test
    void shouldWriteDestinationKeysWithExplicitArrayIndexes() {
        var firstMemberName = new TextInputElement();
        firstMemberName.setId("firstMemberName");
        firstMemberName.setDestinationKey("members.0.first_name");

        var secondTag = new TextInputElement();
        secondTag.setId("secondTag");
        secondTag.setDestinationKey("tags.1");

        var group = new GroupLayoutElement();
        group.setChildren(new LinkedList<>(List.of(firstMemberName, secondTag)));

        var effectiveValues = new EffectiveElementValues();
        effectiveValues.put("firstMemberName", "Ada");
        effectiveValues.put("secondTag", "vip");

        var payload = service.buildPayload(createRoot(group), effectiveValues);

        assertEquals(
                Map.of(
                        "members", List.of(Map.of("first_name", "Ada")),
                        "tags", Arrays.asList(null, "vip")
                ),
                payload
        );
    }

    @Test
    void shouldResolveWildcardDestinationKeysInsideReplicatingContainers() {
        var firstName = new TextInputElement();
        firstName.setId("rowFirstName");
        firstName.setDestinationKey("members.*.first_name");

        var people = new ReplicatingContainerLayoutElement();
        people.setId("people");
        people.setChildren(new LinkedList<>(List.of(firstName)));

        var firstPerson = new EffectiveElementValues();
        firstPerson.put("rowFirstName", "Ada");

        var secondPerson = new EffectiveElementValues();
        secondPerson.put("rowFirstName", "Grace");

        var effectiveValues = new EffectiveElementValues();
        effectiveValues.put("people", List.of(firstPerson, secondPerson));

        var payload = service.buildPayload(createRoot(people), effectiveValues);

        assertEquals(
                Map.of(
                        "members", List.of(
                                Map.of("first_name", "Ada"),
                                Map.of("first_name", "Grace")
                        )
                ),
                payload
        );
    }

    @Test
    void shouldBroadcastWildcardDestinationKeysOutsideReplicatingContainers() {
        var lastName = new TextInputElement();
        lastName.setId("rowLastName");
        lastName.setDestinationKey("last_name");

        var people = new ReplicatingContainerLayoutElement();
        people.setId("people");
        people.setDestinationKey("members");
        people.setChildren(new LinkedList<>(List.of(lastName)));

        var sharedFirstName = new TextInputElement();
        sharedFirstName.setId("sharedFirstName");
        sharedFirstName.setDestinationKey("members.*.first_name");

        var group = new GroupLayoutElement();
        group.setChildren(new LinkedList<>(List.of(people, sharedFirstName)));

        var firstPerson = new EffectiveElementValues();
        firstPerson.put("rowLastName", "Lovelace");

        var secondPerson = new EffectiveElementValues();
        secondPerson.put("rowLastName", "Hopper");

        var effectiveValues = new EffectiveElementValues();
        effectiveValues.put("people", List.of(firstPerson, secondPerson));
        effectiveValues.put("sharedFirstName", "Ada");

        var payload = service.buildPayload(createRoot(group), effectiveValues);

        assertEquals(
                Map.of(
                        "members", List.of(
                                Map.of(
                                        "last_name", "Lovelace",
                                        "first_name", "Ada"
                                ),
                                Map.of(
                                        "last_name", "Hopper",
                                        "first_name", "Ada"
                                )
                        )
                ),
                payload
        );
    }

    @Test
    void shouldBuildEffectiveValuesFromDestinationKeys() {
        var firstName = new TextInputElement();
        firstName.setId("firstName");
        firstName.setDestinationKey("person.first_name");

        var street = new TextInputElement();
        street.setId("street");
        street.setDestinationKey("person.address.street");

        var group = new GroupLayoutElement();
        group.setChildren(new LinkedList<>(List.of(firstName, street)));

        Map<String, Object> payload = Map.of(
                "person", Map.of(
                        "first_name", "Ada",
                        "address", Map.of("street", "Main Street 1")
                )
        );

        var effectiveValues = service.buildEffectiveValues(createRoot(group), payload);

        assertEquals("Ada", effectiveValues.get("firstName"));
        assertEquals("Main Street 1", effectiveValues.get("street"));
    }

    @Test
    void shouldBuildReplicatingContainerEffectiveValuesFromDestinationKeys() {
        var firstName = new TextInputElement();
        firstName.setId("rowFirstName");
        firstName.setDestinationKey("first_name");

        var street = new TextInputElement();
        street.setId("rowStreet");
        street.setDestinationKey("address.street");

        var people = new ReplicatingContainerLayoutElement();
        people.setId("people");
        people.setDestinationKey("payload.people");
        people.setChildren(new LinkedList<>(List.of(firstName, street)));

        Map<String, Object> payload = Map.of(
                "payload", Map.of(
                        "people", List.of(
                                Map.of(
                                        "first_name", "Ada",
                                        "address", Map.of("street", "Main Street 1")
                                ),
                                Map.of(
                                        "first_name", "Grace",
                                        "address", Map.of("street", "Side Alley 2")
                                )
                        )
                )
        );

        var effectiveValues = service.buildEffectiveValues(createRoot(people), payload);

        assertEquals(
                List.of(
                        Map.of(
                                "rowFirstName", "Ada",
                                "rowStreet", "Main Street 1"
                        ),
                        Map.of(
                                "rowFirstName", "Grace",
                                "rowStreet", "Side Alley 2"
                        )
                ),
                effectiveValues.get("people")
        );
    }

    @Test
    void shouldBuildEffectiveValuesForWildcardReplicatingContainersWithoutDestinationKey() {
        var firstName = new TextInputElement();
        firstName.setId("rowFirstName");
        firstName.setDestinationKey("members.*.first_name");

        var tag = new TextInputElement();
        tag.setId("rowTag");
        tag.setDestinationKey("members.*.tags.1");

        var people = new ReplicatingContainerLayoutElement();
        people.setId("people");
        people.setChildren(new LinkedList<>(List.of(firstName, tag)));

        Map<String, Object> payload = Map.of(
                "members", List.of(
                        Map.of(
                                "first_name", "Ada",
                                "tags", Arrays.asList(null, "founder")
                        ),
                        Map.of(
                                "first_name", "Grace",
                                "tags", Arrays.asList(null, "admiral")
                        )
                )
        );

        var effectiveValues = service.buildEffectiveValues(createRoot(people), payload);

        assertEquals(
                List.of(
                        Map.of(
                                "rowFirstName", "Ada",
                                "rowTag", "founder"
                        ),
                        Map.of(
                                "rowFirstName", "Grace",
                                "rowTag", "admiral"
                        )
                ),
                effectiveValues.get("people")
        );
    }

    private static FormLayoutElement createRoot(BaseFormElement child) {
        var step = new GenericStepElement();
        step.setChildren(new LinkedList<>(List.of(child)));

        var root = new FormLayoutElement();
        root.setChildren(new LinkedList<BaseStepElement>(List.of(step)));
        return root;
    }

    private static ComputedElementStates elementStatesWithOverride(String elementId, BaseElement override) {
        var elementStates = new ComputedElementStates();
        elementStates.put(elementId, new ComputedElementState().setOverride(override));
        return elementStates;
    }
}
