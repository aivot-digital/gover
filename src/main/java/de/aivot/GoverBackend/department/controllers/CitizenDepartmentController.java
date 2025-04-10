package de.aivot.GoverBackend.department.controllers;

import de.aivot.GoverBackend.department.dtos.DepartmentResponseDTO;
import de.aivot.GoverBackend.department.services.DepartmentService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nonnull;

@RestController
@RequestMapping("/api/public/departments/")
public class CitizenDepartmentController {

    private final DepartmentService departmentService;

    @Autowired
    public CitizenDepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping("{id}/")
    public DepartmentResponseDTO retrieve(
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        return departmentService
                .retrieve(id)
                .map(DepartmentResponseDTO::fromEntity)
                .orElseThrow(ResponseException::notFound);
    }
}
