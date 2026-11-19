package de.aivot.prosuna.backend.mail.services;

import de.aivot.prosuna.backend.mail.enums.MailTemplate;
import de.aivot.prosuna.backend.system.services.SystemService;
import de.aivot.prosuna.backend.theme.entities.ThemeEntity;
import de.aivot.prosuna.backend.user.entities.UserDeputyEntity;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserDeputyMailServiceTest {
    private final MailService mailService = mock(MailService.class);
    private final UserService userService = mock(UserService.class);
    private final SystemService systemService = mock(SystemService.class);
    private final UserDeputyMailService service = new UserDeputyMailService(mailService, userService, systemService);
    private final UserEntity admin = new UserEntity().setId("admin").setFullName("Alex Beispiel");
    private final UserEntity deputy = new UserEntity().setId("daniel").setFullName("Daniel Müller");
    private final UserEntity original = new UserEntity().setId("philipp").setFullName("Philipp Schröder");
    private final ThemeEntity theme = new ThemeEntity();

    @Test
    @SuppressWarnings("unchecked")
    void tellsTheDeputyWhomTheyRepresentAndForWhichPeriod() throws Exception {
        when(userService.retrieve("daniel")).thenReturn(Optional.of(deputy));
        when(userService.retrieve("philipp")).thenReturn(Optional.of(original));
        when(systemService.retrieveDefaultTheme()).thenReturn(theme);

        service.sendAssigned(admin, relation(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 15)));

        var captor = ArgumentCaptor.forClass(Map.class);
        verify(mailService).sendMailToUser(eq(theme), eq(deputy), eq("[Prosuna] Sie wurden als Stellvertreter:in eingetragen"),
                eq(MailTemplate.DeputyAssigned), captor.capture());
        Map<String, Object> context = captor.getValue();
        assertEquals(original, context.get("originalUser"));
        assertEquals(admin, context.get("admin"));
        assertEquals("vom 02.11.2026 bis 15.11.2026", context.get("periodLabel"));
        assertEquals("/staff/account/deputies", context.get("deputiesPath"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void namesAnUnlimitedPeriod() throws Exception {
        when(userService.retrieve("daniel")).thenReturn(Optional.of(deputy));
        when(userService.retrieve("philipp")).thenReturn(Optional.of(original));

        service.sendAssigned(admin, relation(LocalDate.of(2026, 11, 2), null));

        var captor = ArgumentCaptor.forClass(Map.class);
        verify(mailService).sendMailToUser(any(), eq(deputy), any(), eq(MailTemplate.DeputyAssigned), captor.capture());
        assertEquals("ab 02.11.2026, unbefristet", captor.getValue().get("periodLabel"));
    }

    @Test
    void doesNotNotifyAUserWhoNamedThemselves() throws Exception {
        service.sendAssigned(deputy, relation(LocalDate.of(2026, 11, 2), null));

        verifyNoInteractions(mailService);
    }

    private static UserDeputyEntity relation(LocalDate from, LocalDate until) {
        return new UserDeputyEntity().setId(4).setOriginalUserId("philipp").setDeputyUserId("daniel")
                .setFromDate(from).setUntilDate(until);
    }
}
