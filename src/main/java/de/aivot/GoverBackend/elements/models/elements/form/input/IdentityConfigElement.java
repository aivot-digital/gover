package de.aivot.GoverBackend.elements.models.elements.form.input;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;

import java.util.ArrayList;
import java.util.List;

public class IdentityConfigElement extends BaseInputElement<List<IdentityConfigElementSlot>> {
    public IdentityConfigElement() {
        super(ElementType.IdentityConfig);
    }

    @Override
    public List<IdentityConfigElementSlot> formatValue(Object value) {
        if (value == null) {
            return null;
        }

        var om = ObjectMapperFactory
                .getInstance();

        return switch (value) {
            case List<?> valueList -> {
                List<IdentityConfigElementSlot> list = new ArrayList<>();
                for (Object item : valueList) {
                    list.add(om.convertValue(item, IdentityConfigElementSlot.class));
                }
                yield list;
            }
            case Object[] valueArray -> {
                List<IdentityConfigElementSlot> list = new ArrayList<>();
                for (Object item : valueArray) {
                    list.add(om.convertValue(item, IdentityConfigElementSlot.class));
                }
                yield list;
            }
            case String valueString -> {
                try {
                    yield ObjectMapperFactory
                            .getInstance()
                            .readerForListOf(IdentityConfigElementSlot.class)
                            .readValue(valueString);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            default -> null;
        };
    }

    @Override
    public void performValidation(List<IdentityConfigElementSlot> value) throws ValidationException {
        if (value == null) {
            if (Boolean.TRUE.equals(getRequired())) {
                throw new RequiredValidationException(this);
            }
        }
    }
}
