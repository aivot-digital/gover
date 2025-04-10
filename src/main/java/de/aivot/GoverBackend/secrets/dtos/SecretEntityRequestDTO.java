package de.aivot.GoverBackend.secrets.dtos;

import de.aivot.GoverBackend.lib.ReqeustDTO;
import de.aivot.GoverBackend.secrets.entities.SecretEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

/**
 * This class represents the request data for creating a new secret or updating an existing one.
 */
public record SecretEntityRequestDTO(
        @NotNull(message = "Der Name muss definiert sein")
        @NotBlank(message = "Der Name darf nicht leer sein")
        @Length(min = 3, max = 64, message = "Der Name muss zwischen 3 und 64 Zeichen lang sein")
        String name,

        @NotNull(message = "Die Beschreibung muss definiert sein")
        @NotBlank(message = "Die Beschreibung darf nicht leer sein")
        @Length(max = 255, min = 3, message = "Die Beschreibung muss zwischen 3 und 255 Zeichen lang sein")
        String description,

        @NotNull(message = "Der Wert muss definiert sein")
        @NotBlank(message = "Der Wert darf nicht leer sein")
        String value
) implements ReqeustDTO<SecretEntity> {
        public SecretEntity toEntity() {
                var entity = new SecretEntity();
                entity.setName(name);
                entity.setDescription(description);
                entity.setValue(value);
                return entity;
        }
}
