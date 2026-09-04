package de.aivot.prosuna.backend.payment.xbezahldienste;

import de.aivot.prosuna.backend.payment.exceptions.PaymentException;
import de.aivot.prosuna.backend.payment.models.PaymentAddress;
import de.aivot.prosuna.backend.payment.models.PaymentInformation;
import de.aivot.prosuna.backend.payment.models.PaymentMethod;
import de.aivot.prosuna.backend.payment.models.PaymentRequestItem;
import de.aivot.prosuna.backend.payment.models.PaymentStatus;
import de.aivot.prosuna.backend.xbezahldienste.v1_2_0.Address;
import de.aivot.prosuna.backend.xbezahldienste.v1_2_0.Requestor;

import java.time.ZoneOffset;

public final class XBezahldiensteV120Mapper {
    private XBezahldiensteV120Mapper() {
    }

    public static de.aivot.prosuna.backend.xbezahldienste.v1_2_0.PaymentRequest toExternal(
            de.aivot.prosuna.backend.payment.models.PaymentRequest source
    ) {
        var target = new de.aivot.prosuna.backend.xbezahldienste.v1_2_0.PaymentRequest();
        target.setRequestId(source.requestId());
        target.setRequestTimestamp(source.requestTimestamp().atOffset(ZoneOffset.UTC));
        target.setCurrency(source.currency());
        target.setGrossAmount(source.grossAmount().doubleValue());
        target.setPurpose(source.purpose());
        target.setDescription(source.description());
        target.setRedirectUrl(source.redirectUrl());
        target.setItems(source.items().stream().map(XBezahldiensteV120Mapper::toExternal).toList());
        target.setRequestor(toExternal(source.requestor()));
        return target;
    }

    public static PaymentInformation toDomain(
            de.aivot.prosuna.backend.xbezahldienste.v1_2_0.PaymentInformation source
    ) throws PaymentException {
        try {
            var status = toDomain(source.getStatus());
            return new PaymentInformation(
                    source.getTransactionId(),
                    source.getTransactionReference(),
                    status,
                    status.isPending() ? source.getTransactionRedirectUrl() : null,
                    status.isPaid() && source.getTransactionTimestamp() != null
                            ? source.getTransactionTimestamp().toInstant()
                            : null,
                    status.isPaid() && source.getPaymentMethod() != null
                            ? new PaymentMethod(source.getPaymentMethod().getValue(), source.getPaymentMethodDetail())
                            : null,
                    source.getStatusDetail()
            );
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new PaymentException(e, "Invalid XBezahldienste 1.2.0 payment information");
        }
    }

    private static de.aivot.prosuna.backend.xbezahldienste.v1_2_0.PaymentItem toExternal(PaymentRequestItem source) {
        var target = new de.aivot.prosuna.backend.xbezahldienste.v1_2_0.PaymentItem();
        target.setId(source.id());
        target.setReference(source.reference());
        target.setDescription(source.description());
        target.setTaxRate(source.taxRate().doubleValue());
        target.setQuantity(Math.toIntExact(source.quantity()));
        target.setTotalNetAmount(source.totalNetAmount().doubleValue());
        target.setTotalTaxAmount(source.totalTaxAmount().doubleValue());
        target.setSingleNetAmount(source.singleNetAmount().doubleValue());
        target.setSingleTaxAmount(source.singleTaxAmount().doubleValue());
        target.setBookingData(source.bookingData());
        return target;
    }

    @SuppressWarnings("deprecation")
    private static Requestor toExternal(de.aivot.prosuna.backend.payment.models.PaymentRequestor source) {
        if (source == null) {
            return null;
        }
        var target = new Requestor();
        target.setName(source.name());
        target.setFirstName(source.firstName());
        target.setGender(source.gender() == null ? null : Requestor.GenderEnum.fromValue(source.gender().name()));
        target.setIsOrganization(source.organization());
        target.setOrganizationName(source.organizationName());
        target.setAddress(toExternal(source.address()));
        return target;
    }

    @SuppressWarnings("deprecation")
    private static Address toExternal(PaymentAddress source) {
        if (source == null) {
            return null;
        }
        var target = new Address();
        target.setStreet(source.street());
        target.setHouseNumber(source.houseNumber());
        target.setAddressLine(source.addressLines());
        target.setPostalCode(source.postalCode());
        target.setCity(source.city());
        target.setCountry(source.country());
        return target;
    }

    private static PaymentStatus toDomain(
            de.aivot.prosuna.backend.xbezahldienste.v1_2_0.PaymentInformation.StatusEnum status
    ) {
        return switch (status) {
            case INITIAL -> PaymentStatus.PENDING;
            case PAYED -> PaymentStatus.PAID;
            case FAILED -> PaymentStatus.FAILED;
            case CANCELED -> PaymentStatus.CANCELED;
        };
    }
}
