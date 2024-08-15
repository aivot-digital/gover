package de.aivot.GoverBackend.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.common.contenttype.ContentType;
import de.aivot.GoverBackend.enums.PaymentProvider;
import de.aivot.GoverBackend.enums.PaymentType;
import de.aivot.GoverBackend.enums.SubmissionStatus;
import de.aivot.GoverBackend.enums.XBezahldienstStatus;
import de.aivot.GoverBackend.exceptions.PaymentException;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.models.entities.Destination;
import de.aivot.GoverBackend.models.entities.Form;
import de.aivot.GoverBackend.models.entities.Submission;
import de.aivot.GoverBackend.models.giropay.GiroPayPaymentRequest;
import de.aivot.GoverBackend.models.giropay.GiroPaymentStartResponse;
import de.aivot.GoverBackend.models.xbezahldienst.XBezahldienstePaymentInformation;
import de.aivot.GoverBackend.models.xbezahldienst.XBezahldienstePaymentItem;
import de.aivot.GoverBackend.models.xbezahldienst.XBezahldienstePaymentRequest;
import de.aivot.GoverBackend.models.xbezahldienst.XBezahldienstePaymentTransaction;
import de.aivot.GoverBackend.repositories.DestinationRepository;
import de.aivot.GoverBackend.repositories.FormRepository;
import de.aivot.GoverBackend.repositories.SubmissionAttachmentRepository;
import de.aivot.GoverBackend.repositories.SubmissionRepository;
import de.aivot.GoverBackend.services.mail.ExceptionMailService;
import de.aivot.GoverBackend.services.mail.SubmissionMailService;
import de.aivot.GoverBackend.utils.StringUtils;
import io.netty.handler.codec.base64.Base64Decoder;
import org.apache.commons.io.IOUtils;
import org.eclipse.angus.mail.util.BASE64DecoderStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.script.ScriptEngine;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@EnableScheduling
public class PaymentService {
    private final GoverConfig goverConfig;
    private final ExceptionMailService exceptionMailService;
    private final SubmissionRepository submissionRepository;
    private final SubmissionMailService submissionMailService;
    private final DestinationSubmitService destinationSubmitService;
    private final DestinationRepository destinationRepository;
    private final SubmissionAttachmentRepository submissionAttachmentRepository;
    private final FormRepository formRepository;

    @Autowired
    public PaymentService(GoverConfig goverConfig, ExceptionMailService exceptionMailService, SubmissionRepository submissionRepository, SubmissionMailService submissionMailService, DestinationSubmitService destinationSubmitService, DestinationRepository destinationRepository, SubmissionAttachmentRepository submissionAttachmentRepository, FormRepository formRepository) {
        this.goverConfig = goverConfig;
        this.exceptionMailService = exceptionMailService;
        this.submissionRepository = submissionRepository;
        this.submissionMailService = submissionMailService;
        this.destinationSubmitService = destinationSubmitService;
        this.destinationRepository = destinationRepository;
        this.submissionAttachmentRepository = submissionAttachmentRepository;
        this.formRepository = formRepository;
    }

    @Scheduled(
            cron = "0 * * * * *",
            zone = "Europe/Berlin"
    )
    public void pollPayments() {
        var submissionsToPollPaymentFor = submissionRepository
                .findAllByPaymentRequestIsNotNullAndPaymentErrorIsNullAndStatus(SubmissionStatus.PaymentPending);

        for (var sub : submissionsToPollPaymentFor) {
            if (sub.getPaymentProvider() != PaymentProvider.giroPay) {
                pollSinglePayment(sub);
            }
        }
    }

    public Submission pollSinglePayment(Submission sub) {
        var optForm = formRepository
                .findById(sub.getFormId());

        if (optForm.isEmpty()) {
            // Send exception mail and save error but do not stop the processing of other submissions
            var exception = new PaymentException("Failed to fetch form with id %d for submission %s", sub.getFormId(), sub.getId());
            sub.setPaymentError(exception.getMessage());
            submissionRepository.save(sub);
            exceptionMailService.send(exception);
            return sub;
        }

        var form = optForm.get();

        XBezahldienstePaymentTransaction res;
        try {
            res = pollPayment(form, sub);
        } catch (PaymentException e) {
            // Send exception mail and save error but do not stop the processing of other submissions
            sub.setPaymentError(e.getMessage());
            sub.setStatus(SubmissionStatus.HasPaymentError);
            submissionRepository.save(sub);
            exceptionMailService.send(e);
            return sub;
        } catch (IOException | InterruptedException e) {
            // Ignore this error and try again later
            return sub;
        }

        if (res.getPaymentInformation() == null || res.getPaymentInformation().getStatus() == null) {
            // Send exception mail and save error but do not stop the processing of other submissions
            var exception = new PaymentException("Failed to fetch payment information for submission %s", sub.getId());
            sub.setPaymentError(exception.getMessage());
            sub.setStatus(SubmissionStatus.HasPaymentError);
            submissionRepository.save(sub);
            exceptionMailService.send(exception);
            return sub;
        }

        var sub2 = storePaymentInfoForSubmission(sub, res.getPaymentInformation(), form);

        return submissionRepository.save(sub2);
    }

    public Submission storePaymentInfoForSubmission(Submission sub, XBezahldienstePaymentInformation info, Form form) {
        sub.setPaymentInformation(info);

        if (sub.getPaymentInformation().getStatus() == XBezahldienstStatus.PAYED) {
            sub.setStatus(SubmissionStatus.OpenForManualWork);

            Integer destinationId = form.getDestinationId();
            Destination destination = null;
            if (destinationId != null) {
                var optDestination = destinationRepository
                        .findById(destinationId);
                if (optDestination.isEmpty()) {
                    // Send exception mail and save error but do not stop the processing of other submissions
                    var exception = new RuntimeException("Failed to fetch destination with id " + destinationId + " for submission " + sub.getId());
                    sub.setDestinationResult(exception.getMessage());
                    sub.setStatus(SubmissionStatus.HasDestinationError);
                    submissionRepository.save(sub);
                    exceptionMailService.send(exception);
                    return sub;
                } else {
                    destination = optDestination.get();
                }
            }

            if (destination != null) {
                var attachments = submissionAttachmentRepository
                        .findAllBySubmissionId(sub.getId());

                sub.setDestinationTimestamp(LocalDateTime.now());
                destinationSubmitService.handleSubmit(
                        destination,
                        form,
                        sub,
                        attachments
                );

                if (!sub.getDestinationSuccess()) {
                    try {
                        submissionMailService.sendDestinationFailed(form, sub, destination);
                    } catch (Exception e) {
                        exceptionMailService.send(e);
                    }
                }
            } else {
                try {
                    submissionMailService.sendReceived(form, sub);
                } catch (Exception e) {
                    exceptionMailService.send(e);
                }
            }
        }

        return sub;
    }

    /**
     * Initiate a payment process for a form and a payment request.
     *
     * @param form
     * @param submission
     * @return
     */
    public XBezahldienstePaymentTransaction initiatePayment(Form form, Submission submission) throws IOException, InterruptedException, PaymentException {
        var paymentProvider = getPaymentProvider(submission);

        if (submission.getPaymentProvider() == PaymentProvider.giroPay) {
            return initiateGiroPayPayment(form, submission, paymentProvider);
        }

        var paymentProviderUrl = StringUtils.normalizeUrl(paymentProvider.get("url"));

        var sslContext = getSslContext(paymentProvider);
        var accessToken = getAccessToken(paymentProvider);

        var clientBuilder = HttpClient
                .newBuilder();

        if (sslContext != null) {
            clientBuilder = clientBuilder
                    .sslContext(sslContext);
        }

        var client = clientBuilder
                .build();

        var objectMapper = new ObjectMapper();

        var body = objectMapper
                .writeValueAsString(submission.getPaymentRequest());

        var paymentPath = String
                .format("paymenttransaction/%s/%s", submission.getPaymentOriginatorId(), submission.getPaymentEndpointId());

        var requestBuild = HttpRequest
                .newBuilder(URI.create(paymentProviderUrl + paymentPath))
                .headers("Content-Type", ContentType.APPLICATION_JSON.getType());

        if (accessToken != null) {
            requestBuild = requestBuild
                    .headers("Authorization", "Bearer " + accessToken);
        }

        var request = requestBuild
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        var response = client
                .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new PaymentException(
                    "Payment provider %s for form %s returned status code %d when processing submission %s. Body was %s. Request body was %s",
                    form.getPaymentProvider(),
                    form.getTitle(),
                    response.statusCode(),
                    submission.getId(),
                    response.body(),
                    body
            );
        }

        var transaction = objectMapper
                .readValue(response.body(), XBezahldienstePaymentTransaction.class);

        client.close();

        return transaction;
    }

    private XBezahldienstePaymentTransaction initiateGiroPayPayment(Form form, Submission submission, Map<String, String> paymentProvider) throws IOException, InterruptedException, PaymentException {
        var giroPayPaymentRequest = GiroPayPaymentRequest
                .valueOf(submission.getPaymentRequest(), submission.getPaymentOriginatorId(), submission.getPaymentEndpointId(), paymentProvider.get("projectPassword"));

        var xFormUrlEncoded = giroPayPaymentRequest.toApplicationXWwwFormUrlEncoded();

        var paymentProviderUrl = StringUtils.normalizeUrl(paymentProvider.get("url"));

        var client = HttpClient
                .newBuilder()
                .build();

        var request = HttpRequest
                .newBuilder(URI.create(paymentProviderUrl))
                .headers("Content-Type", ContentType.APPLICATION_URLENCODED.getType())
                .POST(HttpRequest.BodyPublishers.ofString(xFormUrlEncoded))
                .build();

        var response = client
                .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new PaymentException(
                    "Payment provider %s for form %s returned status code %d when processing submission %s. Body was %s. Request body was %s",
                    form.getPaymentProvider(),
                    form.getTitle(),
                    response.statusCode(),
                    submission.getId(),
                    response.body(),
                    xFormUrlEncoded
            );
        }

        var objectMapper = new ObjectMapper();

        var transaction = objectMapper
                .readValue(response.body(), GiroPaymentStartResponse.class);

        client.close();

        if (transaction.getRc() != 0) {
            throw new PaymentException(
                    "Payment provider %s for form %s returned error code %d when processing submission %s. Message was %s. Request body was %s",
                    form.getPaymentProvider(),
                    form.getTitle(),
                    transaction.getRc(),
                    submission.getId(),
                    transaction.getMsg(),
                    xFormUrlEncoded
            );
        }

        return transaction
                .toXBezahldienstePaymentTransaction(submission.getPaymentRequest(), paymentProviderUrl);
    }

    /**
     * Polls the payment transaction information for a submission
     *
     * @param form
     * @param submission
     * @return
     */
    public XBezahldienstePaymentTransaction pollPayment(Form form, Submission submission) throws PaymentException, IOException, InterruptedException {
        var paymentProvider = getPaymentProvider(submission);
        var paymentProviderUrl = StringUtils.normalizeUrl(paymentProvider.get("url"));
        var sslContext = getSslContext(paymentProvider);
        var accessToken = getAccessToken(paymentProvider);

        var clientBuilder = HttpClient
                .newBuilder();

        if (sslContext != null) {
            clientBuilder = clientBuilder
                    .sslContext(sslContext);
        }

        var client = clientBuilder
                .build();

        var paymentPath = String
                .format("paymenttransaction/%s/%s/%s", submission.getPaymentOriginatorId(), submission.getPaymentEndpointId(), submission.getPaymentInformation().getTransactionId());

        var requestBuild = HttpRequest
                .newBuilder(URI.create(paymentProviderUrl + paymentPath));

        if (accessToken != null) {
            requestBuild = requestBuild
                    .headers("Authorization", "Bearer " + accessToken);
        }

        var request = requestBuild
                .GET()
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new PaymentException(
                    "Payment provider %s for form %s returned status code %d when processing submission %s. Body was %s",
                    form.getPaymentProvider(),
                    form.getTitle(),
                    response.statusCode(),
                    submission.getId(),
                    response.body()
            );
        }

        var objectMapper = new ObjectMapper();

        var transaction = objectMapper
                .readValue(response.body(), XBezahldienstePaymentTransaction.class);

        client.close();

        return transaction;
    }

    private SSLContext getSslContext(Map<String, String> paymentProvider) {
        var paymentProviderClientCertPath = paymentProvider.get("certPath");
        var paymentProviderClientCertBase64 = paymentProvider.get("certBase64");
        var paymentProviderClientCertPass = paymentProvider.get("certPassword");

        SSLContext sslContext = null;

        if (StringUtils.isNotNullOrEmpty(paymentProviderClientCertPath)) {
            var password = StringUtils.isNotNullOrEmpty(paymentProviderClientCertPass) ? paymentProviderClientCertPass.toCharArray() : null;
            try {
                KeyStore clientKeyStore = KeyStore.getInstance("PKCS12");
                clientKeyStore.load(new FileInputStream(paymentProviderClientCertPath), password);

                var keyManagerFactory = KeyManagerFactory
                        .getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(clientKeyStore, password);

                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(
                        keyManagerFactory.getKeyManagers(),
                        null,
                        null
                );
            } catch (Exception e) {
                exceptionMailService.send(e);
                throw new RuntimeException(e);
            }
        }

        if (StringUtils.isNotNullOrEmpty(paymentProviderClientCertBase64)) {
            var password = StringUtils.isNotNullOrEmpty(paymentProviderClientCertPass) ? paymentProviderClientCertPass.toCharArray() : null;
            try {
                KeyStore clientKeyStore = KeyStore.getInstance("PKCS12");
                byte[] certBytes = Base64.getDecoder().decode(paymentProviderClientCertBase64);
                InputStream certStream = new ByteArrayInputStream(certBytes);
                clientKeyStore.load(certStream, password);

                var keyManagerFactory = KeyManagerFactory
                        .getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(clientKeyStore, password);

                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(
                        keyManagerFactory.getKeyManagers(),
                        null,
                        null
                );
            } catch (Exception e) {
                exceptionMailService.send(e);
                throw new RuntimeException(e);
            }
        }

        return sslContext;
    }

    private String getAccessToken(Map<String, String> paymentProvider) throws PaymentException, IOException, InterruptedException {
        var paymentProviderOAuthUrl = paymentProvider.get("oauthUrl");
        var paymentProviderClientId = paymentProvider.get("clientId");
        var paymentProviderClientSecret = paymentProvider.get("clientSecret");

        if (StringUtils.isNullOrEmpty(paymentProviderOAuthUrl)) {
            return null; // No oauth configured for this provider
        }

        if (StringUtils.isNullOrEmpty(paymentProviderClientId) ||
                StringUtils.isNullOrEmpty(paymentProviderClientSecret)) {
            throw new PaymentException("OAuth configuration for payment provider %s is incomplete", paymentProvider);
        }

        var client = HttpClient
                .newBuilder()
                .build();

        var formData = String.format("grant_type=client_credentials&client_id=%s&client_secret=%s", paymentProviderClientId, paymentProviderClientSecret);

        var request = HttpRequest
                .newBuilder(URI.create(paymentProviderOAuthUrl))
                .headers("Content-Type", ContentType.APPLICATION_URLENCODED.getType())
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new PaymentException("Failed to get access token from payment provider %s. Status code was %d", paymentProvider, response.statusCode());
        }

        var objectMapper = new ObjectMapper();
        var token = objectMapper
                .readTree(response.body())
                .get("access_token")
                .asText();

        client.close();

        return token;
    }

    private Map<String, String> getPaymentProvider(Submission submission) throws PaymentException {
        var paymentProvider = goverConfig.getPaymentProvider().get(submission.getPaymentProvider().getKey());
        if (paymentProvider == null) {
            throw new PaymentException("Payment provider configuration %s for submission %s not found", submission.getPaymentProvider(), submission.getId());
        }

        if (paymentProvider.get("url") == null) {
            throw new PaymentException("URL for Payment provider %s for submission %s not found", submission.getPaymentProvider(), submission.getId());
        }

        return paymentProvider;
    }

    /**
     * Creates a new payment Request for a submission.
     * Returns an empty optional if no payment is required, either by not setting up a payment provider or by having a total payment sum of 0.
     * Throws a runtime exception if the form or single payment items are not correctly configured.
     *
     * @param form
     * @param scriptEngine
     * @param customerInput
     * @param submissionId
     * @return
     */
    public Optional<XBezahldienstePaymentRequest> createPaymentRequest(Form form, ScriptEngine scriptEngine, Map<String, Object> customerInput, String submissionId) throws PaymentException {
        if (form.getPaymentProvider() == null) {
            return Optional.empty();
        }

        if (StringUtils.isNullOrEmpty(form.getPaymentOriginatorId()) ||
                StringUtils.isNullOrEmpty(form.getPaymentEndpointId()) ||
                StringUtils.isNullOrEmpty(form.getPaymentPurpose()) ||
                form.getProducts() == null ||
                form.getProducts().isEmpty()) {
            throw new PaymentException("Form %s is not correctly configured for payment", form.getTitle());
        }

        // Test if payment provider exists
        var dummySub = new Submission();
        dummySub.setPaymentProvider(form.getPaymentProvider());
        getPaymentProvider(dummySub);

        List<XBezahldienstePaymentItem> items = new LinkedList<>();

        for (var product : form.getProducts()) {
            if (StringUtils.isNullOrEmpty(product.getReference())) {
                throw new PaymentException("Product %s of form %s has no reference", product.getId(), form.getTitle());
            }

            if (StringUtils.isNullOrEmpty(product.getDescription())) {
                throw new PaymentException("Product %s of form %s has no description", product.getId(), form.getTitle());
            }

            if (product.getTaxRate() == null) {
                throw new PaymentException("Product %s of form %s has no tax rate", product.getId(), form.getTitle());
            }

            if (product.getNetPrice() == null) {
                throw new PaymentException("Product %s of form %s has no net price", product.getId(), form.getTitle());
            }

            long quantity = switch (product.getType()) {
                case PaymentType.UpfrontFixed -> {
                    if (product.getUpfrontFixedQuantity() == null) {
                        throw new PaymentException("Product %s of form %s has no fixed upfront quantity", product.getId(), form.getTitle());
                    }
                    yield product.getUpfrontFixedQuantity();
                }
                case PaymentType.UpfrontCalculated -> {
                    if (product.getUpfrontQuantityFunction() == null) {
                        throw new PaymentException("Product %s of form %s has no upfront quantity function", product.getId(), form.getTitle());
                    }

                    var result = product
                            .getUpfrontQuantityFunction()
                            .evaluate(
                                    null,
                                    form.getRoot(),
                                    form.getRoot(),
                                    customerInput,
                                    scriptEngine
                            );

                    if (result == null) {
                        yield 0;
                    }

                    var value = result.getIntegerValue();
                    if (value == null) {
                        throw new PaymentException("Upfront quantity calculation function for product %s of form %s produced no value", product.getId(), form.getTitle());
                    }
                    yield value;
                }
                default -> 0;
            };

            if (quantity > 0) {
                var tax = BigDecimal
                        .valueOf(product.getTaxRate())
                        .setScale(2, RoundingMode.HALF_EVEN);

                var taxRate = tax
                        .divide(BigDecimal.valueOf(100), RoundingMode.HALF_EVEN);

                var netPricePerItem = BigDecimal
                        .valueOf(product.getNetPrice())
                        .setScale(2, RoundingMode.HALF_EVEN);

                var taxPerItem = netPricePerItem
                        .multiply(taxRate)
                        .setScale(2, RoundingMode.HALF_EVEN);

                var totalNetPrice = netPricePerItem
                        .multiply(BigDecimal.valueOf(quantity).setScale(2, RoundingMode.HALF_EVEN))
                        .setScale(2, RoundingMode.HALF_EVEN);

                var totalTax = taxPerItem
                        .multiply(BigDecimal.valueOf(quantity).setScale(2, RoundingMode.HALF_EVEN))
                        .setScale(2, RoundingMode.HALF_EVEN);

                var item = new XBezahldienstePaymentItem();

                item.setId(product.getId());
                item.setReference(product.getReference());
                item.setDescription(product.getDescription());

                item.setQuantity(quantity);

                item.setTaxRate(tax);

                item.setSingleNetAmount(netPricePerItem);
                item.setSingleTaxAmount(taxPerItem);

                item.setTotalNetAmount(totalNetPrice);
                item.setTotalTaxAmount(totalTax);

                var bookingData = new HashMap<String, String>();
                if (product.getBookingData() != null) {
                    for (var dataItem : product.getBookingData()) {
                        bookingData.put(dataItem.key(), dataItem.value());
                    }
                }
                item.setBookingData(bookingData);

                items.add(item);
            }
        }

        if (items.isEmpty()) {
            return Optional.empty();
        }

        var request = new XBezahldienstePaymentRequest();

        request.setRequestId(UUID.randomUUID().toString());
        request.setDescription(form.getPaymentDescription());
        request.setPurpose(form.getPaymentPurpose());

        request.setRequestTimestamp(
                ZonedDateTime
                        .now()
                        .withZoneSameInstant(ZoneOffset.UTC)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
        );

        if (submissionId != null) {
            if (form.getPaymentProvider() == PaymentProvider.giroPay) {
                request.setRedirectUrl(goverConfig.createUrl("/api/public/submissions/" + submissionId + "/giropay/payment-callback"));
            } else {
                request.setRedirectUrl(goverConfig.createUrl("/api/public/submissions/" + submissionId + "/xbezahldienste/payment-callback"));
            }
        } else {
            if (form.getPaymentProvider() == PaymentProvider.giroPay) {
                request.setRedirectUrl(goverConfig.createUrl("/api/public/submissions/xxx/giropay/payment-callback"));
            } else {
                request.setRedirectUrl(goverConfig.createUrl("/api/public/submissions/xxx/xbezahldienste/payment-callback"));
            }
        }

        request.setCurrency("EUR");

        request.setItems(items);
        request.setGrosAmount(items
                .stream()
                .map(i -> i.getTotalNetAmount().add(i.getTotalTaxAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_EVEN)
        );

        if (request.getGrosAmount().equals(BigDecimal.ZERO)) {
            return Optional.empty();
        }

        request.setRequestor(null); // TODO: Set requester correct

        return Optional.of(request);
    }
}
