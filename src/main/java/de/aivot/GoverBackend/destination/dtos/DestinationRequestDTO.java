package de.aivot.GoverBackend.destination.dtos;

import de.aivot.GoverBackend.destination.entities.Destination;
import de.aivot.GoverBackend.destination.enums.DestinationType;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record DestinationRequestDTO(
        @NotNull(message = "Name must be provided")
        @Length(min = 3, max = 255, message = "Name must be between 3 and 255 characters")
        String name,
        @NotNull(message = "Type must be provided")
        DestinationType type,
        String mailTo,
        String mailCC,
        String mailBCC,
        String apiAddress,
        String authorizationHeader,
        Integer maxAttachmentMegaBytes
) {
    public Destination toEntity() {
        Destination destination = new Destination();
        destination.setName(this.name);
        destination.setType(this.type);
        destination.setMailTo(this.mailTo);
        destination.setMailCC(this.mailCC);
        destination.setMailBCC(this.mailBCC);
        destination.setApiAddress(this.apiAddress);
        destination.setAuthorizationHeader(this.authorizationHeader);
        destination.setMaxAttachmentMegaBytes(this.maxAttachmentMegaBytes);
        return destination;
    }
}
