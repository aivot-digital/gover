package de.aivot.prosuna.backend.communication.exceptions;

/** Sending was attempted, but the transport did not return a durable receipt. Never retry automatically. */
public class CommunicationDispatchException extends CommunicationException {
    public CommunicationDispatchException(Throwable cause) {
        super("Der Versandstatus ist ungeklärt. Die Nachricht wurde möglicherweise bereits übergeben. Bitte prüfen Sie den Versand, bevor Sie erneut senden.", cause);
    }
}
