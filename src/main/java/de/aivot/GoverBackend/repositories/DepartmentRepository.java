package de.aivot.GoverBackend.repositories;

import de.aivot.GoverBackend.models.Department;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "departments", path = "departments")
public interface DepartmentRepository extends PagingAndSortingRepository<Department, Long>, CrudRepository<Department, Long> {
}
