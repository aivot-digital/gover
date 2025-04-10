package de.aivot.GoverBackend.payment.controllers.staff;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.exceptions.NotFoundException;
import de.aivot.GoverBackend.exceptions.UnauthorizedException;
import de.aivot.GoverBackend.payment.dtos.PaymentProviderDefinitionResponseDTO;
import de.aivot.GoverBackend.payment.models.PaymentProviderDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;
import java.util.List;

@RestController
@RequestMapping("/api/payment-provider-definitions/")
public class PaymentProviderDefinitionController {
    private final List<PaymentProviderDefinition> paymentProviderDefinitions;

    @Autowired
    public PaymentProviderDefinitionController(List<PaymentProviderDefinition> paymentProviderDefinitions) {
        this.paymentProviderDefinitions = paymentProviderDefinitions;
    }

    @GetMapping("")
    public List<PaymentProviderDefinitionResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return paymentProviderDefinitions
                .stream()
                .map(entity -> {
                    try {
                        return PaymentProviderDefinitionResponseDTO.from(entity);
                    } catch (ResponseException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
    }

    @GetMapping("{key}/")
    public PaymentProviderDefinitionResponseDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nullable @PathVariable String key
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var definition = paymentProviderDefinitions
                .stream()
                .filter(def -> def.getKey().equals(key))
                .findFirst()
                .orElseThrow(ResponseException::notFound);

        return PaymentProviderDefinitionResponseDTO
                .from(definition);
    }
}
