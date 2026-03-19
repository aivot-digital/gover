package de.aivot.GoverBackend.submission.services;

import de.aivot.GoverBackend.elements.models.EffectiveElementValues;
import de.aivot.GoverBackend.elements.models.elements.BaseFormElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.TextInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.FormLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.steps.BaseStepElement;
import de.aivot.GoverBackend.elements.models.elements.steps.GenericStepElement;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DestinationKeyPayloadServiceTest {
    private final ElementDataTransformService service = new ElementDataTransformService();

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

        var payload = Map.of(
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

        var payload = Map.of(
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

        var payload = Map.of(
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
}
