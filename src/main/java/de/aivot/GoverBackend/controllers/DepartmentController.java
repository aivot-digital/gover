package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.enums.UserRole;
import de.aivot.GoverBackend.exceptions.ConflictException;
import de.aivot.GoverBackend.exceptions.ForbiddenException;
import de.aivot.GoverBackend.exceptions.NotFoundException;
import de.aivot.GoverBackend.models.auth.KeyCloakDetailsUser;
import de.aivot.GoverBackend.models.entities.Department;
import de.aivot.GoverBackend.repositories.DepartmentMembershipRepository;
import de.aivot.GoverBackend.repositories.DepartmentRepository;
import de.aivot.GoverBackend.repositories.FormRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Collection;
import java.util.List;

@RestController
public class DepartmentController {
    private final DepartmentRepository departmentRepository;
    private final DepartmentMembershipRepository membershipRepository;
    private final FormRepository formRepository;

    @Autowired
    public DepartmentController(
            DepartmentRepository departmentRepository,
            DepartmentMembershipRepository membershipRepository,
            FormRepository formRepository
    ) {
        this.departmentRepository = departmentRepository;
        this.membershipRepository = membershipRepository;
        this.formRepository = formRepository;
    }

    /**
     * List all departments.
     *
     * @param filterIds A list of ids to filter by.
     * @return A list of all departments.
     */
    @GetMapping("/api/departments")
    public Collection<Department> list(
            @RequestParam(required = false, name = "id") List<Integer> filterIds
    ) {
        if (filterIds != null) {
            return departmentRepository.findAllById(filterIds);
        }

        return departmentRepository.findAll();
    }

    /**
     * Create a new department.
     * Only global admins can create departments.
     *
     * @param jwt           The JWT of the user.
     * @param newDepartment The new department.
     * @return The created department.
     */
    @PostMapping("/api/departments")
    public Department create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody Department newDepartment
    ) {
        KeyCloakDetailsUser
                .fromJwt(jwt)
                .ifNotAdminThrow(ForbiddenException::new);

        return departmentRepository.save(newDepartment);
    }

    /**
     * Retrieve a department by its id.
     *
     * @param id The id of the department.
     * @return The department.
     */
    @GetMapping("/api/departments/{id}")
    public Department retrieve(
            @PathVariable Integer id
    ) {
        return departmentRepository
                .findById(id)
                .orElseThrow(NotFoundException::new);
    }

    /**
     * Retrieve a department by its id as an anonymous user.
     *
     * @param id The id of the department.
     * @return The department.
     */
    @GetMapping("/api/public/departments/{id}")
    public Department retrievePublic(
            @PathVariable Integer id
    ) {
        return departmentRepository
                .findById(id)
                .orElseThrow(NotFoundException::new);
    }

    /**
     * Update a department.
     * Only global admins or department admins can update departments.
     *
     * @param jwt               The JWT of the user.
     * @param id                The id of the department.
     * @param updatedDepartment The updated department.
     * @return The updated department.
     */
    @PutMapping("/api/departments/{id}")
    public Department update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id,
            @Valid @RequestBody Department updatedDepartment
    ) {
        var requestingUser = KeyCloakDetailsUser
                .fromJwt(jwt);

        if (requestingUser.isNotAnAdmin()) {
            var isDepartmentAdmin = membershipRepository
                    .existsByDepartmentIdAndUserIdAndRole(
                            id,
                            requestingUser.getId(),
                            UserRole.Admin
                    );
            if (!isDepartmentAdmin) {
                throw new ForbiddenException();
            }
        }

        var existingDepartment = departmentRepository
                .findById(id)
                .orElseThrow(NotFoundException::new);

        existingDepartment.setName(updatedDepartment.getName());
        existingDepartment.setAddress(updatedDepartment.getAddress());

        existingDepartment.setSpecialSupportAddress(updatedDepartment.getSpecialSupportAddress());
        existingDepartment.setTechnicalSupportAddress(updatedDepartment.getTechnicalSupportAddress());

        existingDepartment.setImprint(updatedDepartment.getImprint());
        existingDepartment.setPrivacy(updatedDepartment.getPrivacy());
        existingDepartment.setAccessibility(updatedDepartment.getAccessibility());

        existingDepartment.setDepartmentMail(updatedDepartment.getDepartmentMail());

        return departmentRepository.save(existingDepartment);
    }

    /**
     * Delete a department.
     * Only global admins can delete departments.
     *
     * @param jwt The JWT of the user.
     * @param id  The id of the department.
     */
    @DeleteMapping("/api/departments/{id}")
    public void destroy(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id
    ) {
        KeyCloakDetailsUser
                .fromJwt(jwt)
                .ifNotAdminThrow(ForbiddenException::new);

        var dep = departmentRepository
                .findById(id)
                .orElseThrow(NotFoundException::new);

        if (formRepository
                .existsByDevelopingDepartmentIdOrManagingDepartmentIdOrResponsibleDepartmentId(dep.getId(), dep.getId(), dep.getId())) {
            throw new ConflictException("Department is still in use");
        }

        departmentRepository.delete(dep);
    }
}
