package de.aivot.GoverBackend.utils;

import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ElementResolver {
    private static final Logger logger = LoggerFactory.getLogger(ElementResolver.class);

    public static BaseElement resolve(Map<String, Object> elementData) {
        var objectMapper = ObjectMapperFactory
                .getInstance();
        try {
            return objectMapper.convertValue(elementData, BaseElement.class);
        } catch (Exception e) {
            logger.error("Failed to resolve element from data: {}", elementData, e);
            return null;
        }
    }
}
