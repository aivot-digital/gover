package de.aivot.prosuna.backend.dataObject.services;

import de.aivot.prosuna.backend.dataObject.entities.DataObjectSchemaEntity;
import de.aivot.prosuna.backend.dataObject.repositories.DataObjectSchemaRepository;
import de.aivot.prosuna.backend.elements.models.elements.BaseFormElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TableInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElement;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DataObjectSchemaServiceTest {
    private final DataObjectSchemaRepository repository = mock(DataObjectSchemaRepository.class);
    private final DataObjectSchemaService service = new DataObjectSchemaService(repository);

    @ParameterizedTest
    @MethodSource("emptySchemas")
    void rejectsMissingDataFieldsOnCreateAndUpdateWithoutChangingExistingModel(GroupLayoutElement schema) {
        var incoming = model(schema).setName("Changed");
        var existingSchema = group(field("name"));
        var existing = model(existingSchema);

        var createError = assertThrows(ResponseException.class, () -> service.create(incoming));
        var updateError = assertThrows(ResponseException.class, () -> service.performUpdate("test", incoming, existing));

        assertEquals(HttpStatus.BAD_REQUEST, createError.getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, updateError.getStatus());
        assertEquals(schema == null
                ? "Bitte legen Sie ein Datenschema mit mindestens einem Datenfeld fest."
                : "Das Datenschema muss mindestens ein Datenfeld enthalten.", createError.getMessage());
        assertEquals(createError.getMessage(), updateError.getMessage());
        assertSame(existingSchema, existing.getSchema());
        assertEquals("Test", existing.getName());
        verify(repository, never()).save(any());
    }

    private static Stream<GroupLayoutElement> emptySchemas() {
        return Stream.of(null, group(), group(group()), group(new ReplicatingContainerLayoutElement()),
                group(group(new ReplicatingContainerLayoutElement())));
    }

    @ParameterizedTest
    @MethodSource("populatedSchemas")
    void acceptsDirectNestedAndRepeatedDataFieldsOnCreateAndUpdate(GroupLayoutElement schema) throws Exception {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var incoming = model(schema);
        var existing = model(group(field("old")));

        assertSame(incoming, service.create(incoming));
        assertSame(existing, service.performUpdate("test", incoming, existing));
        assertSame(schema, existing.getSchema());
        verify(repository, times(2)).save(any());
    }

    private static Stream<GroupLayoutElement> populatedSchemas() {
        var repeated = new ReplicatingContainerLayoutElement();
        repeated.setChildren(List.of(group(field("name"))));
        return Stream.of(group(field("name")), group(group(field("name"))), group(repeated), group(new TableInputElement()));
    }

    @Test
    void acceptsOnlyTheRequiredManualIdFieldAsSchema() throws Exception {
        var idField = field(DataObjectItemService.ID_FIELD_NAME);
        idField.setRequired(true);
        var incoming = model(group(idField)).setIdGen(DataObjectItemService.ID_GEN_CUSTOM);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertSame(incoming, service.create(incoming));
    }

    @Test
    void preservesManualIdValidationWhenAnUpdateSuppliesADifferentGenerator() {
        var idField = field(DataObjectItemService.ID_FIELD_NAME);
        idField.setRequired(true);
        var originalSchema = group(idField);
        var existing = model(originalSchema).setIdGen(DataObjectItemService.ID_GEN_CUSTOM);
        var incoming = model(group(field("name")));

        var error = assertThrows(ResponseException.class, () -> service.performUpdate("test", incoming, existing));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        assertTrue(error.getMessage().contains("obersten Ebene"));
        assertSame(originalSchema, existing.getSchema());
        assertEquals(DataObjectItemService.ID_GEN_CUSTOM, existing.getIdGen());
        verify(repository, never()).save(any());
    }

    private static DataObjectSchemaEntity model(GroupLayoutElement schema) {
        return new DataObjectSchemaEntity().setKey("test").setName("Test").setDescription("Beschreibung")
                .setIdGen(DataObjectItemService.ID_GEN_UUID).setSchema(schema).setDisplayFields(List.of());
    }

    private static GroupLayoutElement group(BaseFormElement... children) {
        return new GroupLayoutElement().setChildren(List.of(children));
    }

    private static TextInputElement field(String id) {
        var field = new TextInputElement();
        field.setId(id);
        return field;
    }
}
