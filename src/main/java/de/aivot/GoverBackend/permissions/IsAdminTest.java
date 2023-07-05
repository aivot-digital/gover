package de.aivot.GoverBackend.permissions;

import de.aivot.GoverBackend.models.entities.User;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Aspect
@Component
public class IsAdminTest {
    @Around("@annotation(IsAdmin)")
    public Object permission(ProceedingJoinPoint joinPoint) throws Throwable {
        for (Object obj : joinPoint.getArgs()) {
            if (obj instanceof Authentication authentication) {
                User requester = (User) authentication.getPrincipal();

                if (requester.isAdmin()) {
                    return joinPoint.proceed();
                }
            }
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
}
