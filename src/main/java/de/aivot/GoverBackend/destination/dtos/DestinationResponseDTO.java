package de.aivot.GoverBackend.destination.dtos;


import de.aivot.GoverBackend.destination.entities.Destination;
import de.aivot.GoverBackend.destination.enums.DestinationType;

import javax.annotation.Nonnull;

public record DestinationResponseDTO(
        @Nonnull
        Integer id,
        @Nonnull
        String name,
        @Nonnull
        DestinationType type,
        @Nonnull
        String mailTo,
        @Nonnull
        String mailCC,
        @Nonnull
        String mailBCC,
        @Nonnull
        String apiAddress,
        @Nonnull
        String authorizationHeader,
        @Nonnull
        Integer maxAttachmentMegaBytes
) {
    public static DestinationResponseDTO fromEntity(Destination destination) {
        return new DestinationResponseDTO(
                destination.getId(),
                destination.getName(),
                destination.getType(),
                destination.getMailTo(),
                destination.getMailCC(),
                destination.getMailBCC(),
                destination.getApiAddress(),
                destination.getAuthorizationHeader(),
                destination.getMaxAttachmentMegaBytes()
        );
    }
}