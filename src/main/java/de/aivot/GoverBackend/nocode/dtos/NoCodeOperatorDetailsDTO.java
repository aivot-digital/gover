package de.aivot.GoverBackend.nocode.dtos;

import de.aivot.GoverBackend.nocode.enums.NoCodeDataType;
import de.aivot.GoverBackend.nocode.models.NoCodeParameter;
import de.aivot.GoverBackend.nocode.providers.NoCodeOperatorServiceProvider;

import java.util.stream.Stream;

/**
 * DTO to send details about an operator to the frontend.
 *
 * @param identifier  The unique identifier of the operator.
 * @param packageName The package name of the operator.
 * @param label       The label of the operator.
 * @param description The description of the operator.
 * @param parameters  The parameters that the operator expects.
 * @param returnType  The type of the return value of the operator.
 */
public record NoCodeOperatorDetailsDTO(
        String identifier,
        String packageName,
        String label,
        String description,
        String abstractDescription,
        NoCodeParameter[] parameters,
        NoCodeDataType returnType
) {
    /**
     * Convert an operator SPI to a stream of DTOs.
     *
     * @param spi The SPI to convert.
     * @return A stream of DTOs.
     */
    public static Stream<NoCodeOperatorDetailsDTO> fromSPI(NoCodeOperatorServiceProvider spi) {
        return Stream
                .of(spi.getOperators())
                .map(op -> new NoCodeOperatorDetailsDTO(
                        spi.getPackageName() + "." + op.getIdentifier(),
                        spi.getPackageName(),
                        op.getLabel(),
                        op.getDescription(),
                        op.getAbstract(),
                        op.getParameters(),
                        op.getReturnType()
                ));
    }
}
