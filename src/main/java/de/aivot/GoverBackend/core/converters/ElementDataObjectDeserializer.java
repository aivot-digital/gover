package de.aivot.GoverBackend.core.converters;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import de.aivot.GoverBackend.config.services.SystemConfigStartupService;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.ElementDataObject;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.enums.ElementType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ElementDataObjectDeserializer extends StdDeserializer<ElementDataObject> {
    private final Logger logger = LoggerFactory.getLogger(ElementDataObjectDeserializer.class);

    public ElementDataObjectDeserializer() {
        super(ElementDataObject.class);
    }

    protected ElementDataObjectDeserializer(Class<ElementDataObject> vc) {
        super(vc);
    }

    @Override
    public ElementDataObject deserialize(JsonParser jp,
                                         DeserializationContext ctx) throws IOException, JacksonException {
        try {
            return getElementDataObject(jp, ctx);
        } catch (Exception ex) {
            logger.error("Could not deserialize ElementDataObject", ex);
            throw ex;
        }
    }

    private ElementDataObject getElementDataObject(JsonParser jp, DeserializationContext ctx) throws IOException {
        JsonNode node = jp
                .getCodec()
                .readTree(jp);

        Function<String, Boolean> getBoolean = (key) -> {
            var valueNode = node.get(key);
            if (valueNode == null || valueNode.isNull() || !valueNode.isBoolean()) {
                return null;
            }
            return valueNode.asBoolean();
        };

        var typeNode = node
                .get("$type");

        if (typeNode == null || typeNode.isNull() || !typeNode.isIntegralNumber()) {
            throw new JsonMappingException(jp, "$type is not a number. $type is " + (typeNode == null ? "NULL" : typeNode.toString()));
        }

        var typeOpt = ElementType
                .findElement(typeNode.asInt());

        if (typeOpt.isEmpty()) {
            throw new JsonMappingException(jp, "$type is not a valid ElementType");
        }

        var type = typeOpt.get();

        var ed = new ElementDataObject(type)
                .setIsVisible(getBoolean.apply("isVisible"))
                .setIsPrefilled(getBoolean.apply("isPrefilled"))
                .setIsDirty(getBoolean.apply("isDirty"));

        var computedErrorsNode = node
                .get("computedErrors");
        if (computedErrorsNode != null && !computedErrorsNode.isNull() && computedErrorsNode.isArray()) {
            var errorList = new LinkedList<String>();
            computedErrorsNode
                    .forEach(errorNode -> {
                        if (errorNode != null && !errorNode.isNull() && errorNode.isTextual()) {
                            errorList.add(errorNode.asText());
                        }
                    });
            ed.setComputedErrors(errorList);
        }

        var computedOverrideNode = node
                .get("computedOverride");
        if (computedOverrideNode != null && !computedOverrideNode.isNull() && computedOverrideNode.isObject()) {
            var computedOverride = ctx
                    .readTreeAsValue(computedOverrideNode, BaseElement.class);
            ed.setComputedOverride(computedOverride);
        }

        var inputValueNode = node
                .get("inputValue");
        if (inputValueNode != null && !inputValueNode.isNull()) {
            ed.setInputValue(parseValueNode(jp, type, inputValueNode, ctx));
        }

        var computedValueNode = node
                .get("computedValue");
        if (computedValueNode != null && !computedValueNode.isNull()) {
            ed.setComputedValue(parseValueNode(jp, type, computedValueNode, ctx));
        }

        return ed;
    }

    private Object parseValueNode(JsonParser jp,
                                  ElementType currentElementType,
                                  JsonNode inputValueNode,
                                  DeserializationContext ctx) throws IOException {
        if (inputValueNode.isTextual()) {
            return inputValueNode.asText();
        } else if (inputValueNode.isNumber()) {
            return inputValueNode.numberValue();
        } else if (inputValueNode.isBoolean()) {
            return inputValueNode.asBoolean();
        } else if (inputValueNode.isObject()) {
            return ctx.readTreeAsValue(inputValueNode, Map.class);
        } else if (inputValueNode.isArray()) {
            if (currentElementType == ElementType.ReplicatingContainer) {
                var collected = new LinkedList<Object>();

                var iter = inputValueNode.elements();
                while (iter.hasNext()) {
                    var childNode = iter.next();
                    try {
                        var childElementData = ctx.readTreeAsValue(childNode, ElementData.class);
                        collected.add(childElementData);
                    } catch (Exception e) {
                        throw e;
                    }
                }

                return collected;
            } else {
                return ctx.readTreeAsValue(inputValueNode, List.class);
            }
        } else {
            throw new JsonMappingException(jp, "The node is not a valid value type: " + inputValueNode);
        }
    }
}
