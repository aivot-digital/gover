package de.aivot.GoverBackend.payment.controllers.staff;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.payment.dtos.PaymentTransactionResponseDTO;
import de.aivot.GoverBackend.payment.exceptions.PaymentException;
import de.aivot.GoverBackend.payment.filters.PaymentTransactionFilter;
import de.aivot.GoverBackend.payment.services.PaymentProviderTestService;
import de.aivot.GoverBackend.payment.services.PaymentTransactionService;
import de.aivot.GoverBackend.user.services.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map;

@RestController
public class PaymentTransactionController {
    private final GoverConfig goverConfig;
    private final PaymentTransactionService paymentTransactionService;

    @Autowired
    public PaymentTransactionController(
            GoverConfig goverConfig,
            PaymentTransactionService paymentTransactionService
    ) {
        this.goverConfig = goverConfig;
        this.paymentTransactionService = paymentTransactionService;
    }

    @GetMapping("/api/payment-transactions/")
    public Page<PaymentTransactionResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid PaymentTransactionFilter filter
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return paymentTransactionService
                .list(pageable, filter)
                .map(PaymentTransactionResponseDTO::fromEntity);
    }

    @GetMapping("/api/payment-transactions/{key}/")
    public PaymentTransactionResponseDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String key

    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return paymentTransactionService
                .retrieve(key)
                .map(PaymentTransactionResponseDTO::fromEntity)
                .orElseThrow(ResponseException::notFound);
    }

    /**
     * This endpoint is used to handle the notify callback from the payment provider.
     * This endpoint should handle the processing of the callback data.
     * When calling this endpoint, inform the calling payment provider that the callback data has been received.
     * <br/>
     * ATTENTION: When this path changes, make sure to change the notify url creation at {@link de.aivot.GoverBackend.core.payment.girocheckoutPaymentProviderDefinition#initiatePayment}.
     *
     * @param transactionKey The transaction key
     * @param callbackData   The callback data in the query params received from the payment provider.
     * @throws ResponseException If an error occurs while processing the callback data
     */
    @GetMapping("/api/public/payment-transaction-callback/{transactionKey}/notify/")
    public void getCallbackNotify(
            @Nonnull @PathVariable String transactionKey,
            @Nullable @RequestParam Map<String, Object> callbackData
    ) throws ResponseException {
        // Test if the transaction key is the test key. If so, return.
        if (PaymentProviderTestService.TestRedirectID.equalsIgnoreCase(transactionKey)) {
            return;
        }

        // Check if the callback data is empty. If so, throw a bad request exception.
        if (callbackData == null || callbackData.isEmpty()) {
            throw ResponseException.badRequest("Keine Zahlungsinformationen erhalten");
        }

        // Fetch the payment transaction by the transaction key
        var transaction = paymentTransactionService
                .retrieve(transactionKey)
                .orElseThrow(ResponseException::notFound);

        // Try to process the callback data. If an exception is thrown, catch it and throw an internal server error exception.
        try {
            paymentTransactionService
                    .processCallback(transaction, callbackData);
        } catch (PaymentException e) {
            throw ResponseException.internalServerError("Zahlungsinformationen konnten nicht verarbeitet werden.", e);
        }
    }

    /**
     * This endpoint is used to handle the redirect callback from the payment provider.
     * This endpoint should handle the processing of the callback data.
     * When calling this endpoint, redirect the user to the given redirect url of the saved transaction.
     * <br/>
     * ATTENTION: When this path changes, make sure to change the notify url creation at {@link de.aivot.GoverBackend.core.payment.girocheckoutPaymentProviderDefinition#initiatePayment}.
     *
     * @param transactionKey The transaction key
     * @param callbackData   The callback data in the query params received from the payment provider.
     * @param response       The response object to redirect the user to the saved transaction's redirect url.
     * @throws IOException       If an I/O error occurs
     * @throws ResponseException If an error occurs while processing the callback data
     */
    @GetMapping("/api/public/payment-transaction-callback/{transactionKey}/redirect/")
    public void getCallbackRedirect(
            @Nonnull @PathVariable String transactionKey,
            @Nullable @RequestParam Map<String, Object> callbackData,
            @Nonnull HttpServletResponse response
    ) throws IOException, ResponseException {
        // Test if the transaction key is the test key. If so, return a plain text response with "OK" and a status code of 200.
        if (PaymentProviderTestService.TestRedirectID.equalsIgnoreCase(transactionKey)) {
            response.getWriter().write("Test erfolgreich!");
            response.setStatus(HttpStatus.OK.value());
            return;
        }

        // Fetch the payment transaction by the transaction key
        var transaction = paymentTransactionService
                .retrieve(transactionKey)
                .orElseThrow(ResponseException::notFound);

        // Check if callback data is null or empty. If so, set it to null.
        var cleanedData = (callbackData == null || callbackData.isEmpty()) ? null : callbackData;

        try {
            paymentTransactionService
                    .processCallback(transaction, cleanedData);
        } catch (PaymentException e) {
            throw ResponseException.internalServerError("Fehler beim abrufen der Zahlungsinformationen", e);
        }

        var redirect = goverConfig
                .createUrl(transaction.getRedirectUrl());

        response.sendRedirect(redirect);
    }
}
