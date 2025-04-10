package de.aivot.GoverBackend.destination.repositories;

import de.aivot.GoverBackend.destination.entities.Destination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DestinationRepository extends JpaRepository<Destination, Integer>, JpaSpecificationExecutor<Destination> {
}
