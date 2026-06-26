package de.aivot.GoverBackend.nocode.dtos;

import de.aivot.GoverBackend.nocode.models.NoCodeSignatur;
import de.aivot.GoverBackend.nocode.providers.NoCodeOperatorsProvider;

import java.util.stream.Stream;

/**
 * DTO to send details about an operator to the frontend.
 *
 * @param identifier  The unique identifier of the operator.
 * @param packageName The plugin component key providing the operator.
 * @param label       The label of the operator.
 * @param description The description of the operator.
 * @param signatures  The signatures of the operator.
 */
public record NoCodeOperatorDetailsDTO(
        String identifier,
        String packageName,
        String label,
        String description,
        String abstractDescription,
        String humanReadableTemplate,
        String[] tags,
        NoCodeSignatur[] signatures
) {
    /**
     * Convert an operator SPI to a stream of DTOs.
     *
     * @param spi The SPI to convert.
     * @return A stream of DTOs.
     */
    public static Stream<NoCodeOperatorDetailsDTO> fromSPI(NoCodeOperatorsProvider spi) {
        return Stream
                .of(spi.getOperators())
                .map(op -> new NoCodeOperatorDetailsDTO(
                        op.getIdentifier(),
                        spi.getKey(),
                        op.getLabel(),
                        op.getDescription(),
                        op.getAbstract(),
                        op.getHumanReadableTemplate(),
                        op.getTags(),
                        op.getSignatures()
                ));
    }
}
