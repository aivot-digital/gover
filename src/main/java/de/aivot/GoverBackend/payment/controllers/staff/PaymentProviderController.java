package de.aivot.GoverBackend.payment.controllers.staff;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.filters.FormFilter;
import de.aivot.GoverBackend.form.repositories.FormRepository;
import de.aivot.GoverBackend.form.services.FormRevisionService;
import de.aivot.GoverBackend.form.services.FormService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.payment.dtos.PaymentProviderRequestDTO;
import de.aivot.GoverBackend.payment.dtos.PaymentProviderResponseDTO;
import de.aivot.GoverBackend.payment.dtos.PaymentProviderTestDataRequestDTO;
import de.aivot.GoverBackend.payment.dtos.PaymentProviderTestDataResponseDTO;
import de.aivot.GoverBackend.payment.entities.PaymentProviderEntity;
import de.aivot.GoverBackend.payment.filters.PaymentProviderFilter;
import de.aivot.GoverBackend.payment.services.PaymentProviderService;
import de.aivot.GoverBackend.payment.services.PaymentProviderTestService;
import de.aivot.GoverBackend.user.services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

@RestController
@RequestMapping("/api/payment-providers/")
public class PaymentProviderController {
    private final ScopedAuditService auditService;

    private final PaymentProviderService paymentProviderService;
    private final PaymentProviderTestService paymentProviderTestService;
    private final FormRepository formRepository;
    private final FormRevisionService formRevisionService;

    @Autowired
    public PaymentProviderController(
            AuditService auditService,
            PaymentProviderService paymentProviderService,
            PaymentProviderTestService paymentProviderTestService,
            FormRepository formRepository,
            FormRevisionService formRevisionService) {
        this.auditService = auditService.createScopedAuditService(PaymentProviderController.class);
        this.paymentProviderService = paymentProviderService;
        this.paymentProviderTestService = paymentProviderTestService;
        this.formRepository = formRepository;
        this.formRevisionService = formRevisionService;
    }

    @GetMapping("")
    public Page<PaymentProviderResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid PaymentProviderFilter filter
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return paymentProviderService
                .list(pageable, filter)
                .map(PaymentProviderResponseDTO::fromEntity);
    }

    @PostMapping("")
    public PaymentProviderResponseDTO create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid PaymentProviderRequestDTO requestDTO
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        var created = paymentProviderService
                .create(requestDTO.toEntity());

        auditService
                .logAction(user, AuditAction.Create, PaymentProviderEntity.class, Map.of(
                        "key", created.getKey(),
                        "name", created.getName()
                ));

        return PaymentProviderResponseDTO
                .fromEntity(created);
    }

    @GetMapping("{key}/")
    public PaymentProviderResponseDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String key
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return paymentProviderService
                .retrieve(key)
                .map(PaymentProviderResponseDTO::fromEntity)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{key}/")
    public PaymentProviderResponseDTO update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String key,
            @Nonnull @RequestBody @Valid PaymentProviderRequestDTO requestDTO
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        var existing = paymentProviderService
                .retrieve(key)
                .orElseThrow(ResponseException::notFound);

        if (existing.getIsEnabled() && !requestDTO.isEnabled()) {
            var filterAllPublishedForms = FormFilter
                    .create()
                    .setPaymentProvider(key)
                    .setStatus(FormStatus.Published)
                    .build();

            if (formRepository.exists(filterAllPublishedForms)) {
                throw ResponseException.conflict(
                        "Der Zahlungsanbieter kann nicht deaktiviert werden, da er noch in einem oder mehreren Formularen verwendet wird."
                );
            } else {
                var filterAllRelatedForms = FormFilter
                        .create()
                        .setPaymentProvider(key)
                        .build();

                for (var form : formRepository.findAll(filterAllRelatedForms)) {
                    var formClone = form.clone();

                    form.setPaymentProvider(null);
                    formRepository.save(form);

                    formRevisionService
                            .create(user, form, formClone);
                }
            }
        }

        var result = paymentProviderService
                .update(key, requestDTO.toEntity());

        auditService
                .logAction(user, AuditAction.Update, PaymentProviderEntity.class, Map.of(
                        "key", result.getKey(),
                        "name", result.getName()
                ));

        return PaymentProviderResponseDTO
                .fromEntity(result);
    }

    @DeleteMapping("{key}/")
    public void destroy(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String key
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        var deleted = paymentProviderService
                .delete(key);

        auditService
                .logAction(user, AuditAction.Delete, PaymentProviderEntity.class, Map.of(
                        "key", deleted.getKey(),
                        "name", deleted.getName()
                ));
    }

    @PostMapping("{key}/test/")
    public PaymentProviderTestDataResponseDTO test(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String key,
            @RequestBody @Valid PaymentProviderTestDataRequestDTO requestDTO
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        var result = paymentProviderTestService.test(
                key,
                requestDTO.purpose(),
                requestDTO.description(),
                requestDTO.amount()
        );

        return new PaymentProviderTestDataResponseDTO(
                result.ok(),
                result.request(),
                result.transaction(),
                result.errorMessage()
        );
    }
}
