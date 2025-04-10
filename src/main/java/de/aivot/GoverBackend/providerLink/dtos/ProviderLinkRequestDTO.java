package de.aivot.GoverBackend.providerLink.dtos;

import de.aivot.GoverBackend.lib.ReqeustDTO;
import de.aivot.GoverBackend.providerLink.entities.ProviderLink;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.URL;

public record ProviderLinkRequestDTO(
        @NotNull
        @NotBlank(message = "Text cannot be blank")
        @Length(min = 1, max = 128, message = "Text must be between 1 and 128 characters")
        String text,

        @NotNull
        @NotBlank(message = "Link cannot be blank")
        @Length(min = 1, max = 128, message = "Link must be between 1 and 128 characters")
        @URL(message = "Link must be a valid URL")
        String link
) implements ReqeustDTO<ProviderLink> {
    @Override
    public ProviderLink toEntity() {
        var providerLink = new ProviderLink();
        providerLink.setText(text());
        providerLink.setLink(link());
        return providerLink;
    }
}
