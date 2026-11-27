package de.aivot.prosuna.backend.mail.services;

import org.junit.jupiter.api.Test;
import jakarta.mail.MessagingException;
import static org.junit.jupiter.api.Assertions.*;

class MailRecipientAddressTest {
    @Test void normalizesEachRecipientWithoutBreakingListsOrDisplayNames() throws Exception {
        var recipients = MailService.parseRecipientAddresses("Person <Person@büro.de>, intern@verwaltung");
        assertEquals(2, recipients.length);
        assertEquals("Person", recipients[0].getPersonal());
        assertEquals("Person@xn--bro-hoa.de", recipients[0].getAddress());
        assertEquals("intern@verwaltung", recipients[1].getAddress());
    }
    @Test void preservesRecipientGroups() throws Exception {
        var group = MailService.parseRecipientAddresses("Team: person@büro.de, intern@verwaltung;")[0];
        assertTrue(group.isGroup());
        assertEquals("person@xn--bro-hoa.de", group.getGroup(true)[0].getAddress());
        assertEquals("intern@verwaltung", group.getGroup(true)[1].getAddress());
    }
    @Test void rejectsHeaderInjection() {
        assertThrows(MessagingException.class, () -> MailService.parseRecipientAddresses("a@example.org\r\nBcc: b@example.org"));
    }
    @Test void reportsInvalidAddressesThroughTheMailExceptionContract() {
        assertThrows(MessagingException.class, () -> MailService.parseRecipientAddresses("person"));
    }
}
