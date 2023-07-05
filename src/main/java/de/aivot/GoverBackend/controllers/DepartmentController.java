package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.enums.UserRole;
import de.aivot.GoverBackend.models.entities.AccessibleDepartment;
import de.aivot.GoverBackend.models.entities.Department;
import de.aivot.GoverBackend.models.entities.User;
import de.aivot.GoverBackend.permissions.IsAdmin;
import de.aivot.GoverBackend.repositories.AccessibleDepartmentRepository;
import de.aivot.GoverBackend.repositories.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@RestController
public class DepartmentController {
    private final DepartmentRepository departmentRepository;
    private final AccessibleDepartmentRepository accessibleDepartmentRepository;

    @Autowired
    public DepartmentController(
            DepartmentRepository departmentRepository,
            AccessibleDepartmentRepository accessibleDepartmentRepository
    ) {
        this.departmentRepository = departmentRepository;
        this.accessibleDepartmentRepository = accessibleDepartmentRepository;
    }

    @GetMapping("/api/departments")
    public Collection<Department> list(
            Authentication authentication,
            @RequestParam(required = false) Boolean member,
            @RequestParam(required = false) Integer roleId
    ) {
        var requester = (User) authentication.getPrincipal();

        Optional<UserRole> role = roleId != null ? Arrays.stream(UserRole.values()).filter(r -> r.matches(roleId)).findFirst() : Optional.empty();

        Collection<AccessibleDepartment> accessibleDepartments = null;
        if (Boolean.TRUE.equals(member) && role.isPresent()) {
            accessibleDepartments = accessibleDepartmentRepository
                    .findAllByUserIdAndRole(requester.getId(), role.get());
        } else if (Boolean.TRUE.equals(member)) {
            accessibleDepartments = accessibleDepartmentRepository
                    .findAllByUserId(requester.getId());
        } else if (role.isPresent()) {
            accessibleDepartments = accessibleDepartmentRepository
                    .findAllByRole(role.get());
        }

        if (accessibleDepartments != null) {
            var accessibleIds = accessibleDepartments
                    .stream()
                    .map(AccessibleDepartment::getDepartmentId)
                    .toList();
            return departmentRepository.findAllByIdIn(accessibleIds);
        }

        return departmentRepository.findAll();
    }

    @IsAdmin
    @PostMapping("/api/departments")
    public Department create(
            Authentication authentication,
            @RequestBody Department newDepartment
    ) {
        return departmentRepository.save(newDepartment);
    }

    @GetMapping("/api/departments/{id}")
    public Department retrieve(
            @PathVariable Integer id
    ) {
        return departmentRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/api/public/departments/{id}")
    public Department retrievePublic(
            @PathVariable Integer id
    ) {
        return departmentRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/api/departments/{id}")
    public Department update(
            Authentication authentication,
            @PathVariable Integer id,
            @RequestBody Department updatedDepartment
    ) {
        var requester = (User) authentication.getPrincipal();

        if (!requester.isAdmin()) {
            var isAdminMember = accessibleDepartmentRepository.existsByDepartmentIdAndUserIdAndRole(
                    id,
                    requester.getId(),
                    UserRole.Admin
            );
            if (!isAdminMember) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }

        Optional<Department> optDepartment = departmentRepository.findById(id);
        if (optDepartment.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Department existingDepartment = optDepartment.get();

        existingDepartment.setName(updatedDepartment.getName());
        existingDepartment.setAddress(updatedDepartment.getAddress());

        existingDepartment.setSpecialSupportAddress(updatedDepartment.getSpecialSupportAddress());
        existingDepartment.setTechnicalSupportAddress(updatedDepartment.getTechnicalSupportAddress());

        existingDepartment.setImprint(updatedDepartment.getImprint());
        existingDepartment.setPrivacy(updatedDepartment.getPrivacy());
        existingDepartment.setAccessibility(updatedDepartment.getAccessibility());

        return departmentRepository.save(existingDepartment);
    }

    @IsAdmin
    @DeleteMapping("/api/departments/{id}")
    public void destroy(
            Authentication authentication,
            @PathVariable Integer id
    ) {
        var dep = departmentRepository.findById(id);

        if (dep.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        departmentRepository.delete(dep.get());
    }
}
