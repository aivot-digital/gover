package de.aivot.GoverBackend.repositories;

import de.aivot.GoverBackend.models.Destination;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "destinations", path = "destinations")
public interface DestinationRepository extends PagingAndSortingRepository<Destination, Long>, CrudRepository<Destination, Long> {
}
