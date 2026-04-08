package de.aivot.GoverBackend.elements.utils;

import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.GoverBackend.elements.annotations.InputElementPOJOBinding;
import de.aivot.GoverBackend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.GoverBackend.elements.annotations.ReplicatingContainerLayoutElementElementPOJOBinding;
import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.EffectiveElementValues;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.LayoutElement;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.utils.ReflectionUtils;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;

import java.lang.reflect.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A mapper for converting effective element values to POJOs and vice versa.
 */
public class ElementPOJOMapper {
    /**
     * Convert the given effective element values to an instance of the pojoClass. Based on the annotations present in the pojoClass, the method maps the data from the effective
     * values to the fields of the pojoClass. See {@link InputElementPOJOBinding} and {@link LayoutElementPOJOBinding} for more details on the annotations used for mapping.
     */
    public static <T> T mapToPOJO(EffectiveElementValues effectiveElementValues, Class<T> pojoClass) throws ElementDataConversionException {
        // Create an instance of the target class to fill the fields into
        T target = instantiateTargetClass(pojoClass);

        // Get a list of all fields in the target class.
        var fields = ReflectionUtils.getAllDeclaredFields(pojoClass);

        // Iterate over all fields and set their values based on the annotations present
        for (var field : fields) {
            Object valueToSet;

            // Check if the field has the relevant annotations and extract the value accordingly
            if (field.isAnnotationPresent(InputElementPOJOBinding.class)) {
                valueToSet = extractInputFieldValue(effectiveElementValues, field);
            } else if (List.class.isAssignableFrom(field.getType())) {
                // Get the generic type of the list. This represents the item type for the replicating container datasets.
                var genericTypeClass = ReflectionUtils
                        .getGenericType(field, 0)
                        .orElse(null);

                // Check if the generic type class has the ReplicatingContainerLayoutElementElementPOJOBinding annotation
                if (genericTypeClass == null || !genericTypeClass.isAnnotationPresent(ReplicatingContainerLayoutElementElementPOJOBinding.class)) {
                    continue;
                }

                valueToSet = extractReplicatingContainerLayoutValue(effectiveElementValues, field, genericTypeClass);
            } else if (field.getType().isAnnotationPresent(LayoutElementPOJOBinding.class)) {
                valueToSet = mapToPOJO(effectiveElementValues, field.getType());
            } else {
                // No relevant annotation present, skip this field
                continue;
            }

            // If the field is public, set the value directly.
            // If not, use the setter method.
            if (Modifier.isPublic(field.getModifiers())) {
                try {
                    field.set(target, valueToSet);
                } catch (IllegalAccessException e) {
                    throw new ElementDataConversionException(
                            "Field %s of class %s is not accessible.",
                            StringUtils.quote(field.getName()),
                            StringUtils.quote(pojoClass.getCanonicalName())
                    );
                }
            } else {
                // Set the extracted value to the field using its setter method
                try {
                    var setterMethod = getSetterMethodForField(pojoClass, field);
                    setterMethod.invoke(target, valueToSet);
                } catch (IllegalAccessException e) {
                    throw new ElementDataConversionException(
                            "Setter method %s for field %s of class %s is not accessible.",
                            StringUtils.quote(StringUtils.getSetterMethodName(field.getName())),
                            StringUtils.quote(field.getName()),
                            StringUtils.quote(pojoClass.getCanonicalName())
                    );
                } catch (InvocationTargetException e) {
                    throw new ElementDataConversionException(
                            "Setter method %s for field %s of class %s threw an exception: %s",
                            StringUtils.quote(StringUtils.getSetterMethodName(field.getName())),
                            StringUtils.quote(field.getName()),
                            StringUtils.quote(pojoClass.getCanonicalName()),
                            e.getTargetException().getMessage()
                    );
                }
            }
        }

        // Return the populated target instance
        return target;
    }

    /**
     * Extract the child items for a replicating container layout pojo. This always returns a {@link List} consisting of the child item pojo types.
     *
     * @param effectiveElementValues The effective values to extract the values from.
     * @param field                  The field to map the values to.
     * @return The extracted list of child item pojos.
     * @throws ElementDataConversionException If there is a type mismatch or other error during extraction.
     */
    private static List<Object> extractReplicatingContainerLayoutValue(@Nonnull EffectiveElementValues elementData,
                                                                       @Nonnull Field field,
                                                                       @Nonnull Class<?> itemClass) throws ElementDataConversionException {
        var annotation = itemClass
                .getAnnotation(ReplicatingContainerLayoutElementElementPOJOBinding.class);

        List<?> childElementData = (List<?>) elementData
                .getOrDefault(annotation.id(), new LinkedList<>());

        List<Object> results = new LinkedList<>();
        for (Object childElementDataObject : childElementData) {
            if (childElementDataObject instanceof Map<?, ?> cd) {
                var converted = mapToPOJO((EffectiveElementValues) cd, itemClass);
                results.add(converted);
            } else {
                throw new ElementDataConversionException(
                        "Type mismatch for field %s of class %s: expected effective element values but got %s",
                        StringUtils.quote(field.getName()),
                        StringUtils.quote(field.getDeclaringClass().getCanonicalName()),
                        childElementDataObject.getClass().getSimpleName()
                );
            }
        }

        return results;
    }

    /**
     * Extract the value for a field annotated with @InputElementPOJOBinding from the given effective element values.
     *
     * @param elementValues The effective element values.
     * @param field         The field to extract the value for.
     * @return The extracted value for the field.
     * @throws ElementDataConversionException If there is a type mismatch or other error during extraction.
     */
    private static Object extractInputFieldValue(@Nonnull EffectiveElementValues elementValues,
                                                 @Nonnull Field field) throws ElementDataConversionException {
        var annotation = field
                .getAnnotation(InputElementPOJOBinding.class);

        var id = annotation.id();

        var valueObj = elementValues
                .getOrDefault(id, null);

        return ObjectMapperFactory
                .getInstance()
                .convertValue(valueObj, field.getType());
    }

    public static <T extends LayoutElement<C>, C extends BaseElement, S> T createFromPOJO(Class<S> pojoClass) throws ElementDataConversionException {
        ElementType targetType;
        String targetId;
        ElementPOJOBindingProperty[] targetProperties;

        if (pojoClass.isAnnotationPresent(ReplicatingContainerLayoutElementElementPOJOBinding.class)) {
            var classAnnotation = pojoClass
                    .getAnnotation(ReplicatingContainerLayoutElementElementPOJOBinding.class);

            targetType = ElementType.ReplicatingContainerLayout;
            targetId = classAnnotation.id();
            targetProperties = classAnnotation.properties();
        } else if (pojoClass.isAnnotationPresent(LayoutElementPOJOBinding.class)) {
            var classAnnotation = pojoClass
                    .getAnnotation(LayoutElementPOJOBinding.class);

            targetType = classAnnotation.type();
            targetId = classAnnotation.id();
            targetProperties = classAnnotation.properties();
        } else {
            throw new ElementDataConversionException(
                    "Source class %s is not annotated with any known POJO binding annotation.",
                    StringUtils.quote(pojoClass.getCanonicalName())
            );
        }

        T target;
        try {
            target = (T) ElementType
                    .getElementClass(targetType)
                    .setType(targetType)
                    .setId(targetId);
        } catch (ClassCastException e) {
            throw new ElementDataConversionException(
                    "Element type %s is not a layout element.",
                    StringUtils.quote(targetType.name())
            );
        }

        try {
            applyPropertiesToElement(target, targetProperties);
        } catch (NoSuchFieldException | InvocationTargetException | IllegalAccessException e) {
            throw new ElementDataConversionException(
                    "Failed to apply properties to element %s of class %s: %s",
                    StringUtils.quote(targetId),
                    StringUtils.quote(pojoClass.getCanonicalName()),
                    e.getMessage()
            );
        }

        // Get a list of all fields in the target class.
        var fields = ReflectionUtils.getAllDeclaredFields(pojoClass);

        for (var field : fields) {
            BaseElement fieldElement;
            if (field.isAnnotationPresent(InputElementPOJOBinding.class)) {
                var annotation = field
                        .getAnnotation(InputElementPOJOBinding.class);

                fieldElement = ElementType
                        .getElementClass(annotation.type())
                        .setType(annotation.type())
                        .setId(annotation.id());

                try {
                    applyPropertiesToElement(fieldElement, annotation.properties());
                } catch (NoSuchFieldException | InvocationTargetException | IllegalAccessException e) {
                    throw new ElementDataConversionException(
                            "Failed to apply properties to element %s of class %s: %s",
                            StringUtils.quote(annotation.id()),
                            StringUtils.quote(pojoClass.getCanonicalName()),
                            e.getMessage()
                    );
                }
            } else if (List.class.isAssignableFrom(field.getType())) {
                var genericTypeClass = ReflectionUtils
                        .getGenericType(field, 0)
                        .orElse(null);

                if (genericTypeClass == null || !genericTypeClass.isAnnotationPresent(ReplicatingContainerLayoutElementElementPOJOBinding.class)) {
                    continue;
                }

                var annotation = genericTypeClass
                        .getAnnotation(ReplicatingContainerLayoutElementElementPOJOBinding.class);

                fieldElement = createFromPOJO(genericTypeClass);

                try {
                    applyPropertiesToElement(fieldElement, annotation.properties());
                } catch (NoSuchFieldException | InvocationTargetException | IllegalAccessException e) {
                    throw new ElementDataConversionException(
                            "Failed to apply properties to element %s of class %s: %s",
                            StringUtils.quote(annotation.id()),
                            StringUtils.quote(pojoClass.getCanonicalName()),
                            e.getMessage()
                    );
                }
            } else if (field.getType().isAnnotationPresent(LayoutElementPOJOBinding.class)) {
                var annotation = field
                        .getType()
                        .getAnnotation(LayoutElementPOJOBinding.class);

                var layoutPojoType = field
                        .getType();

                fieldElement = createFromPOJO(layoutPojoType);

                try {
                    applyPropertiesToElement(fieldElement, annotation.properties());
                } catch (NoSuchFieldException | InvocationTargetException | IllegalAccessException e) {
                    throw new ElementDataConversionException(
                            "Failed to apply properties to element %s of class %s: %s",
                            StringUtils.quote(annotation.id()),
                            StringUtils.quote(pojoClass.getCanonicalName()),
                            e.getMessage()
                    );
                }
            } else {
                // No relevant annotation present, skip this field
                continue;
            }

            target.addChild((C) fieldElement);
        }

        return target;
    }

    private static void applyPropertiesToElement(@Nonnull Object target,
                                                 @Nonnull ElementPOJOBindingProperty[] properties) throws NoSuchFieldException, ElementDataConversionException, InvocationTargetException, IllegalAccessException {
        var targetClass = target
                .getClass();

        for (var property : properties) {
            var field = ReflectionUtils
                    .getDeclaredField(targetClass, property.key())
                    .orElseThrow(() -> new NoSuchFieldException(
                            String.format(
                                    "No field %s found in class %s.",
                                    StringUtils.quote(property.key()),
                                    StringUtils.quote(targetClass.getCanonicalName())
                            )
                    ));
            var setterMethod = getSetterMethodForField(targetClass, field);

            if (!StringUtils.isNullOrEmpty(property.strValue())) {
                setterMethod.invoke(
                        target,
                        property.strValue()
                );
            } else if (property.doubleValue() != Double.MIN_VALUE) {
                setterMethod.invoke(
                        target,
                        property.doubleValue()
                );
            } else if (property.intValue() != Integer.MIN_VALUE) {
                setterMethod.invoke(
                        target,
                        property.intValue()
                );
            } else if (property.boolValue()) {
                setterMethod.invoke(
                        target,
                        property.boolValue()
                );
            } else {
                setterMethod.invoke(target, (Object) null);
            }
        }
    }

    /**
     * Instantiate the target class using its no-argument constructor.
     *
     * @param targetClass The target class to instantiate.
     * @param <T>         The type of the target class.
     * @return An instance of the target class.
     * @throws ElementDataConversionException If there is an error during instantiation.
     */
    private static <T> @Nonnull T instantiateTargetClass(Class<T> targetClass) throws ElementDataConversionException {
        Constructor<T> targetClassConstructor;
        try {
            targetClassConstructor = targetClass.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new ElementDataConversionException(
                    "The target class %s must have a public no-argument constructor.",
                    StringUtils.quote(targetClass.getCanonicalName())
            );
        }

        T target;
        try {
            target = targetClassConstructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new ElementDataConversionException(
                    "Failed to instantiate target class %s: %s",
                    StringUtils.quote(targetClass.getCanonicalName()),
                    e.getMessage()
            );
        }

        return target;
    }

    /**
     * Retrieve the setter method for a given field in the target class.
     *
     * @param targetClass The target class containing the field.
     * @param field       The field for which to retrieve the setter method.
     * @param <T>         The type of the target class.
     * @return The setter method for the field.
     * @throws ElementDataConversionException If the setter method is not found.
     */
    private static <T> Method getSetterMethodForField(Class<T> targetClass, Field field) throws ElementDataConversionException {
        // Determine the setter method name for the field
        var setterMethodName = StringUtils
                .getSetterMethodName(field.getName());

        // Retrieve the setter method for the field
        Method setterMethod;
        try {
            setterMethod = targetClass
                    .getMethod(setterMethodName, field.getType());
        } catch (NoSuchMethodException e) {
            throw new ElementDataConversionException(
                    "No setter method %s found for field %s of class %s.",
                    StringUtils.quote(setterMethodName),
                    StringUtils.quote(field.getName()),
                    StringUtils.quote(targetClass.getCanonicalName())
            );
        }

        return setterMethod;
    }

}
