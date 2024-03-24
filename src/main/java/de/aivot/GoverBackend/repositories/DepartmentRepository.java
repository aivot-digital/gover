package de.aivot.GoverBackend.repositories;

import de.aivot.GoverBackend.models.entities.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;


public interface DepartmentRepository extends JpaRepository<Department, Integer> {
    Collection<Department> findAllByIdIn(List<Integer> ids);
}
