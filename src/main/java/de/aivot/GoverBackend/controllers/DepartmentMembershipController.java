package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.enums.UserRole;
import de.aivot.GoverBackend.models.dtos.DepartmentMembershipDto;
import de.aivot.GoverBackend.models.entities.DepartmentMembership;
import de.aivot.GoverBackend.models.entities.User;
import de.aivot.GoverBackend.repositories.AccessibleDepartmentRepository;
import de.aivot.GoverBackend.repositories.DepartmentMembershipRepository;
import de.aivot.GoverBackend.repositories.DepartmentRepository;
import de.aivot.GoverBackend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Optional;

@RestController
public class DepartmentMembershipController {
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final AccessibleDepartmentRepository accessibleDepartmentRepository;
    private final DepartmentMembershipRepository membershipRepository;

    @Autowired
    public DepartmentMembershipController(
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            AccessibleDepartmentRepository accessibleDepartmentRepository, DepartmentMembershipRepository membershipRepository
    ) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.accessibleDepartmentRepository = accessibleDepartmentRepository;
        this.membershipRepository = membershipRepository;
    }

    @GetMapping("/api/department-memberships")
    public Collection<DepartmentMembershipDto> list(
            Authentication authentication,
            @RequestParam(required = false) Integer department,
            @RequestParam(required = false) Integer user
    ) {
        var requester = (User) authentication.getPrincipal();

        Collection<DepartmentMembership> memberships;
        if (user != null && department != null) {
            memberships = membershipRepository.findAllByUserIdAndDepartmentId(user, department);
        } else if (user != null) {
            memberships = membershipRepository.findAllByUserId(user);
        } else if (department != null) {
            memberships = membershipRepository.findAllByDepartmentId(department);
        } else if (requester.isAdmin()) {
            memberships = membershipRepository.findAll();
        } else {
            memberships = membershipRepository.findAllByUserId(requester.getId());
        }

        return memberships
                .stream()
                .map(DepartmentMembershipDto::valueOf)
                .toList();
    }

    @PostMapping("/api/department-memberships")
    public DepartmentMembershipDto create(
            Authentication authentication,
            @RequestBody DepartmentMembershipDto newMembership
    ) {
        var requester = (User) authentication.getPrincipal();

        if (!requester.isAdmin()) {
            boolean isAdminMember = accessibleDepartmentRepository.existsByDepartmentIdAndUserIdAndRole(
                    newMembership.getDepartment(),
                    requester.getId(),
                    UserRole.Admin
            );
            if (!isAdminMember) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }

        var user = userRepository
                .findById(newMembership.getUser());
        if (user.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        var department = departmentRepository
                .findById(newMembership.getDepartment());
        if (department.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        var membershipAlreadyExists = membershipRepository
                .existsByUserIdAndDepartmentId(user.get().getId(), department.get().getId());
        if (membershipAlreadyExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        DepartmentMembership membership = new DepartmentMembership();
        membership.setDepartment(department.get());
        membership.setUser(user.get());
        membership.setRole(newMembership.getRole());

        return DepartmentMembershipDto.valueOf(membershipRepository.save(membership));
    }

    @DeleteMapping("/api/department-memberships/{id}")
    public void destroy(
            Authentication authentication,
            @PathVariable Integer id
    ) {
        User requester = (User) authentication.getPrincipal();

        if (!requester.isAdmin()) {
            boolean isAdminMember = accessibleDepartmentRepository.existsByDepartmentIdAndUserIdAndRole(
                    id,
                    requester.getId(),
                    UserRole.Admin
            );
            if (!isAdminMember) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }

        Optional<DepartmentMembership> membershipToDelete = membershipRepository.findById(id);
        if (membershipToDelete.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        membershipRepository.delete(membershipToDelete.get());
    }
}
