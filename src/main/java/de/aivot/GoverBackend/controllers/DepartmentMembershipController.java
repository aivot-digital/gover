package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.enums.UserRole;
import de.aivot.GoverBackend.exceptions.*;
import de.aivot.GoverBackend.models.auth.KeyCloakDetailsUser;
import de.aivot.GoverBackend.models.entities.DepartmentMembership;
import de.aivot.GoverBackend.repositories.DepartmentMembershipRepository;
import de.aivot.GoverBackend.repositories.DepartmentRepository;
import de.aivot.GoverBackend.repositories.UserRepository;
import de.aivot.GoverBackend.services.mail.DepartmentMembershipMailService;
import de.aivot.GoverBackend.services.mail.ExceptionMailService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collection;

@RestController
public class DepartmentMembershipController {
    private final DepartmentRepository departmentRepository;
    private final DepartmentMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final DepartmentMembershipMailService departmentMembershipMailService;
    private final ExceptionMailService exceptionMailService;

    @Autowired
    public DepartmentMembershipController(
            DepartmentRepository departmentRepository,
            DepartmentMembershipRepository membershipRepository,
            UserRepository userRepository,
            DepartmentMembershipMailService departmentMembershipMailService,
            ExceptionMailService exceptionMailService
    ) {
        this.departmentRepository = departmentRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.departmentMembershipMailService = departmentMembershipMailService;
        this.exceptionMailService = exceptionMailService;
    }

    /**
     * List all department memberships.
     *
     * @param departmentId The department id to filter by.
     * @param userId       The user id to filter by.
     * @param userRole     The user role to filter by.
     * @return A list of department memberships.
     */
    @GetMapping("/api/department-memberships")
    public Collection<DepartmentMembership> list(
            @RequestParam(required = false, name = "department") Integer departmentId,
            @RequestParam(required = false, name = "user") String userId,
            @RequestParam(required = false, name = "role") UserRole userRole
    ) {
        Collection<DepartmentMembership> memberships;

        if (departmentId != null && userId != null && userRole != null) {
            memberships = membershipRepository.findAllByDepartmentIdAndUserIdAndRole(departmentId, userId, userRole);
        } else if (departmentId != null && userId != null) {
            memberships = membershipRepository.findAllByDepartmentIdAndUserId(departmentId, userId);
        } else if (departmentId != null && userRole != null) {
            memberships = membershipRepository.findAllByDepartmentIdAndRole(departmentId, userRole);
        } else if (userId != null && userRole != null) {
            memberships = membershipRepository.findAllByUserIdAndRole(userId, userRole);
        } else if (departmentId != null) {
            memberships = membershipRepository.findAllByDepartmentId(departmentId);
        } else if (userId != null) {
            memberships = membershipRepository.findAllByUserId(userId);
        } else if (userRole != null) {
            memberships = membershipRepository.findAllByRole(userRole);
        } else {
            memberships = membershipRepository.findAll();
        }

        return memberships;
    }

    /**
     * Create a new department membership.
     *
     * @param jwt           The JWT of the requesting user.
     * @param newMembership The new membership to create.
     * @return The created membership.
     */
    @PostMapping("/api/department-memberships")
    public DepartmentMembership create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DepartmentMembership newMembership
    ) {
        var requestingUser = KeyCloakDetailsUser
                .fromJwt(jwt);

        if (requestingUser.isNotAnAdmin()) {
            var isDepartmentAdmin = membershipRepository
                    .existsByDepartmentIdAndUserIdAndRole(
                            newMembership.getDepartmentId(),
                            requestingUser.getId(),
                            UserRole.Admin
                    );
            if (!isDepartmentAdmin) {
                throw new ForbiddenException();
            }
        }

        var user = userRepository
                .getUser(newMembership.getUserId(), jwt)
                .orElseThrow(NotFoundException::new);

        var department = departmentRepository
                .findById(newMembership.getDepartmentId())
                .orElseThrow(BadRequestException::new);

        var membershipAlreadyExists = membershipRepository
                .existsByDepartmentIdAndUserId(
                        department.getId(),
                        user.getId()
                );
        if (membershipAlreadyExists) {
            throw new ConflictException();
        }

        DepartmentMembership membership = new DepartmentMembership();
        membership.setDepartmentId(department.getId());
        membership.setUserId(user.getId());
        membership.setRole(newMembership.getRole());

        if (!requestingUser.getId().equals(user.getId())) {
            try {
                departmentMembershipMailService.sendAdded(
                        requestingUser,
                        user,
                        department,
                        newMembership
                );
            } catch (MessagingException | IOException e) {
                throw new RuntimeException(e);
            } catch (InvalidUserEMailException e) {
                exceptionMailService.send(e);
            }
        }

        return membershipRepository.save(membership);
    }

    /**
     * Update an existing department membership.
     *
     * @param jwt               The JWT of the requesting user.
     * @param id                The id of the membership to update.
     * @param updatedMembership The updated membership.
     * @return The updated membership.
     */
    @PutMapping("/api/department-memberships/{id}")
    public DepartmentMembership update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id,
            @Valid @RequestBody DepartmentMembership updatedMembership
    ) {
        var membershipToUpdate = membershipRepository
                .findById(id)
                .orElseThrow(NotFoundException::new);

        var requestingUser = KeyCloakDetailsUser
                .fromJwt(jwt);

        if (requestingUser.isNotAnAdmin()) {
            var isDepartmentAdmin = membershipRepository
                    .existsByDepartmentIdAndUserIdAndRole(
                            membershipToUpdate.getDepartmentId(),
                            requestingUser.getId(),
                            UserRole.Admin
                    );
            if (!isDepartmentAdmin) {
                throw new ForbiddenException();
            }
        }

        membershipToUpdate.setRole(updatedMembership.getRole());

        return membershipRepository.save(membershipToUpdate);
    }

    /**
     * Delete an existing department membership.
     *
     * @param jwt The JWT of the requesting user.
     * @param id  The id of the membership to delete.
     */
    @DeleteMapping("/api/department-memberships/{id}")
    public void destroy(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id
    ) {
        var membershipToDelete = membershipRepository
                .findById(id)
                .orElseThrow(NotFoundException::new);

        var requestingUser = KeyCloakDetailsUser
                .fromJwt(jwt);

        if (requestingUser.isNotAnAdmin()) {
            var isDepartmentAdmin = membershipRepository
                    .existsByDepartmentIdAndUserIdAndRole(
                            membershipToDelete.getDepartmentId(),
                            requestingUser.getId(),
                            UserRole.Admin
                    );
            if (!isDepartmentAdmin) {
                throw new ForbiddenException();
            }
        }

        var activeSubmissionsExists = membershipRepository
                .existsActiveSubmissionByUserIdAndDepartmentId(
                        membershipToDelete.getUserId(),
                        membershipToDelete.getDepartmentId()
                );

        if (activeSubmissionsExists) {
            throw new ConflictException();
        }

        var user = userRepository
                .getUser(membershipToDelete.getUserId(), jwt);

        var department = departmentRepository
                .findById(membershipToDelete.getDepartmentId())
                .orElseThrow(NotFoundException::new);

        if (user.isPresent() && !requestingUser.getId().equals(user.get().getId())) {
            try {
                departmentMembershipMailService.sendRemoved(
                        requestingUser,
                        user.get(),
                        department,
                        membershipToDelete
                );
            } catch (MessagingException | IOException e) {
                throw new RuntimeException(e);
            } catch (InvalidUserEMailException e) {
                exceptionMailService.send(e);
            }
        }

        membershipRepository.delete(membershipToDelete);
    }
}
